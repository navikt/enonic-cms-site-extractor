package no.nav.migration

import kotlinx.coroutines.*
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocumentBuilder
import no.nav.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.OpenSearchContentDocumentBuilder
import java.util.*


class CmsMigrator(
    private val params: ICmsMigrationParams,
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    private val jobId: UUID = UUID.randomUUID()

    private var job: Job? = null

    val status: CmsMigratorStatus = CmsMigratorStatus(
        params,
        CmsMigrationDocumentCounter(params, cmsClient)
            .runCount()
            .getCount()
    )

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run() {
        if (status.state != CmsMigratorState.NOT_STARTED) {
            status.log("Attempted to start migrator for ${params.key} (current state is ${status.state}")
            return
        }

        status.log("Starting migration for ${params.key}")

        job = GlobalScope.launch {
            runJob()
        }
    }

    private suspend fun runJob() {
        status.state = CmsMigratorState.RUNNING

        try {
            when (params) {
                is CmsCategoryMigrationParams -> {
                    migrateCategory(
                        params.key,
                        params.withChildren ?: false,
                        params.withContent ?: false,
                        params.withVersions ?: false,
                    )
                }

                is CmsContentMigrationParams -> {
                    migrateContent(
                        params.key,
                        params.withVersions ?: false,
                    )
                }

                is CmsVersionMigrationParams -> {
                    migrateVersion(params.key)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                status.log("Job for ${params.key} was cancelled")
            } else {
                status.log("Exception while running job for ${params.key} - ${e.message}", true)
                status.state = CmsMigratorState.FAILED
            }
            throw e
        }

        status.log("Finished running job for ${params.key}")
        status.state = CmsMigratorState.FINISHED
    }

    suspend fun abort() {
        status.state = CmsMigratorState.ABORTED
        status.log("Sending cancel signal for ${params.key}")
        job?.cancelAndJoin()
    }

    private suspend fun migrateCategory(
        categoryKey: Int,
        withChildren: Boolean,
        withContent: Boolean,
        withVersions: Boolean,
    ) {
        val categoryDocument = OpenSearchCategoryDocumentBuilder(cmsClient).build(categoryKey)

        if (categoryDocument == null) {
            status.log("Failed to create category document for $categoryKey", true)
            return
        }

        val result = openSearchClient.indexCategoryDocument(categoryDocument)

        status.setResult(
            categoryKey,
            ElementType.CATEGORY,
            "Result for category $categoryKey to ${result.index}: ${result.result}"
        )

        if (withContent) {
            categoryDocument.contents?.forEach {
                migrateContent(it.key.toInt(), withVersions)
            }
        }

        if (withChildren) {
            categoryDocument.categories?.forEach {
                migrateCategory(it.key.toInt(), true, withContent, withVersions)
            }
        }

        this.status.categoriesMigrated++
    }

    private suspend fun migrateContent(
        contentKey: Int,
        withVersions: Boolean,
    ) {
        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(contentKey)

        if (contentDocument == null) {
            status.log("Failed to create content document with content key $contentKey", true)
            return
        }

        val result = openSearchClient.indexContentDocument(contentDocument)

        status.setResult(
            contentKey,
            ElementType.CONTENT,
            "Result for content $contentKey to ${result.index}: ${result.result}"
        )

        migrateBinaries(contentDocument)

        this.status.contentsMigrated++

        if (withVersions) {
            contentDocument.versions?.forEach {
                if (it.key == contentDocument.versionKey) {
                    return
                }

                migrateVersion(it.key.toInt())
            }
        }
    }

    private suspend fun migrateVersion(versionKey: Int) {
        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)

        if (contentVersionDocument == null) {
            status.log("Failed to create content document with version key $versionKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentVersionDocument)

        status.setResult(
            versionKey,
            ElementType.VERSION,
            "Result for content version $versionKey to ${result.index}: ${result.result}"
        )

        migrateBinaries(contentVersionDocument)

        this.status.versionsMigrated++
    }

    private suspend fun migrateBinaries(contentDocument: OpenSearchContentDocument) {
        val binaryRefs = contentDocument.binaries ?: return

        val contentKey = contentDocument.contentKey.toInt()
        val versionKey = contentDocument.versionKey.toInt()

        binaryRefs.forEach {
            val binaryDocument = OpenSearchBinaryDocumentBuilder(cmsClient)
                .build(
                    it,
                    contentKey = contentKey,
                    versionKey = versionKey
                )

            val binaryKey = it.key.toInt()

            if (binaryDocument == null) {
                status.log(
                    "Failed to create binary document with key $binaryKey for content $contentKey ($versionKey)",
                    true
                )
                return
            }

            val result = openSearchClient.indexBinaryDocument(binaryDocument)

            status.setResult(
                binaryKey,
                ElementType.BINARY,
                "Result for binary with key $binaryKey for content $contentKey ($versionKey): ${result.result}"
            )

            this.status.binariesMigrated++
        }
    }
}
