package no.nav.migration

import kotlinx.coroutines.*
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocumentBuilder
import no.nav.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.OpenSearchContentDocumentBuilder


class CmsMigrator(
    private val params: ICmsMigrationParams,
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    private var job: Job? = null

    val status: CmsMigratorStatus = CmsMigratorStatus(
        params, CmsMigrationDocumentCounter(params, cmsClient)
            .runCount()
            .getCount()
    )

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run(): String {
        if (status.state == CmsMigratorState.RUNNING) {
            return "Migrator for ${params.key} is already running"
        }

        val msg = "Starting migration for ${params.key}";

        status.log(msg)

        job = GlobalScope.launch {
            runJob()
        }

        return msg
    }

    private suspend fun runJob() {
        status.state = CmsMigratorState.RUNNING

        try {
            when (params) {
                is CmsCategoryMigrationParams -> {
                    val categoryProgress = CmsCategoryMigrationStatus(params.key)
                    status.categoriesStatus.add(categoryProgress)
                    migrateCategory(
                        params.key,
                        params.withChildren ?: false,
                        params.withContent ?: false,
                        params.withVersions ?: false,
                        categoryProgress
                    )
                }

                is CmsContentMigrationParams -> {
                    val contentProgress = CmsContentMigrationStatus(params.key)
                    status.contentStatus.add(contentProgress)
                    migrateContent(
                        params.key,
                        params.withVersions ?: false,
                        contentProgress
                    )
                }

                is CmsVersionMigrationParams -> {
                    val versionProgress = CmsVersionMigrationStatus(params.key)
                    status.versionsStatus.add(versionProgress)
                    migrateVersion(params.key, versionProgress)
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
        status: CmsCategoryMigrationStatus
    ) {
        val categoryDocument = OpenSearchCategoryDocumentBuilder(cmsClient).build(categoryKey)

        if (categoryDocument == null) {
            status.log("Failed to create category document for $categoryKey", true)
            return
        }

        status.numCategories = categoryDocument.categories?.size ?: 0
        status.numContent = categoryDocument.contents?.size ?: 0

        val result = openSearchClient.indexCategoryDocument(categoryDocument)

        status.log("Result for category $categoryKey to ${result.index}: ${result.result}")

        if (withContent) {
            categoryDocument.contents?.forEach {
                val key = it.key.toInt()
                val contentProgress = CmsContentMigrationStatus(key)
                status.contentStatus.add(contentProgress)
                migrateContent(key, withVersions, contentProgress)
            }
        }

        if (withChildren) {
            categoryDocument.categories?.forEach {
                val key = it.key.toInt()
                val categoryProgress = CmsCategoryMigrationStatus(key)
                status.categoriesStatus.add(categoryProgress)
                migrateCategory(key, true, withContent, withVersions, categoryProgress)
            }
        }
    }

    private suspend fun migrateContent(
        contentKey: Int,
        withVersions: Boolean,
        status: CmsContentMigrationStatus
    ) {
        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(contentKey)

        if (contentDocument == null) {
            status.log("Failed to create content document with content key $contentKey")
            return
        }

        status.numVersions = contentDocument.versions?.size ?: 0

        val result = openSearchClient.indexContentDocument(contentDocument)

        status.log("Result for content $contentKey to ${result.index}: ${result.result}")

        migrateBinaries(contentDocument, status)

        if (withVersions) {
            contentDocument.versions?.forEach {
                if (it.key == contentDocument.versionKey) {
                    return
                }

                val key = it.key.toInt()
                val versionProgress = CmsVersionMigrationStatus(key)
                status.versionsStatus.add(versionProgress)
                migrateVersion(key, versionProgress)
            }
        }
    }

    private suspend fun migrateVersion(versionKey: Int, status: CmsVersionMigrationStatus) {
        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)

        if (contentVersionDocument == null) {
            status.log("Failed to create content document with version key $versionKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentVersionDocument)

        status.log("Result for content version $versionKey to ${result.index}: ${result.result}")

        migrateBinaries(contentVersionDocument, status)
    }

    private suspend fun migrateBinaries(contentDocument: OpenSearchContentDocument, status: CmsMigratorStatusBase) {
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
                status.log("Failed to create binary document with key $binaryKey for content $contentKey ($versionKey)")
                return
            }

            val result = openSearchClient.indexBinaryDocument(binaryDocument)

            status.log("Result for binary with key $binaryKey for content $contentKey ($versionKey): ${result.result}")
        }
    }
}
