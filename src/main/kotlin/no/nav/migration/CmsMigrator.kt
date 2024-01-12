package no.nav.migration

import kotlinx.coroutines.*
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocumentBuilder
import no.nav.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.OpenSearchContentDocumentBuilder


enum class CmsMigratorState {
    NOT_STARTED, RUNNING, ABORTED, FAILED, FINISHED
}

private val finishedStates = listOf(CmsMigratorState.FINISHED, CmsMigratorState.FAILED, CmsMigratorState.ABORTED)

class CmsMigrator(
    private val params: ICmsMigrationParams,
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    var state: CmsMigratorState = CmsMigratorState.NOT_STARTED
    private var job: Job? = null

    private val status: CmsMigrationStatus = CmsMigrationStatus(
        params,
        cmsClient,
        openSearchClient
    )

    suspend fun run() {
        if (state != CmsMigratorState.NOT_STARTED) {
            status.log("Attempted to start migration for ${params.key}, but it was already started - current state: $state")
            return
        }

        status.log("Starting migration job for ${params.key}")

        job = runBlocking {
            launch {
                runJob()
            }
        }
    }

    private suspend fun setState(newState: CmsMigratorState) {
        status.log("Setting migration state to $newState")
        state = newState

        if (newState in finishedStates) {
            status.persistToDb()
        }
    }

    private suspend fun runJob() {
        setState(CmsMigratorState.RUNNING)

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
                setState(CmsMigratorState.ABORTED)
            } else {
                status.log("Exception while running job for ${params.key} - ${e.message}", true)
                setState(CmsMigratorState.FAILED)
            }
            throw e
        }

        setState(CmsMigratorState.FINISHED)
    }

    suspend fun abort() {
        status.log("Sending cancel signal for ${params.key}")
        job?.cancelAndJoin()
    }

    fun getStatus(withResults: Boolean? = false, withRemaining: Boolean? = false): CmsMigrationStatusData {
        return status.getStatus(withResults, withRemaining)
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

        val result = openSearchClient.indexCategory(categoryDocument)

        status.setResult(
            categoryKey,
            CmsElementType.CATEGORY,
            result
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

        val result = openSearchClient.indexContent(contentDocument)

        status.setResult(
            contentKey,
            CmsElementType.CONTENT,
            result
        )

        migrateBinaries(contentDocument)

        if (withVersions) {
            contentDocument.versions?.forEach {
                if (it.key != contentDocument.versionKey) {
                    migrateVersion(it.key.toInt())
                }
            }
        }
    }

    private suspend fun migrateVersion(versionKey: Int) {
        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)

        if (contentVersionDocument == null) {
            status.log("Failed to create content document with version key $versionKey", true)
            return
        }

        val result = openSearchClient.indexContent(contentVersionDocument)

        status.setResult(
            versionKey,
            CmsElementType.VERSION,
            result,
            " for content ${contentVersionDocument.contentKey}"
        )

        migrateBinaries(contentVersionDocument)
    }

    private suspend fun migrateBinaries(contentDocument: OpenSearchContentDocument) {
        val binaryRefs = contentDocument.binaries ?: return

        val contentKey = contentDocument.contentKey.toInt()
        val versionKey = contentDocument.versionKey.toInt()

        binaryRefs.forEach {
            val binaryKey = it.key.toInt()

            if (status.binariesIndexed.contains(binaryKey)) {
                val result = openSearchClient.addVersionKeyToBinaryDocument(binaryKey, versionKey)
                if (result == null) {
                    status.log("Failed to add new versionKey to binary $binaryKey", true)
                }
                return@forEach
            }

            val binaryDocument = OpenSearchBinaryDocumentBuilder(cmsClient)
                .build(
                    it,
                    contentKey = contentKey,
                    versionKey = versionKey
                )

            if (binaryDocument == null) {
                status.log(
                    "Failed to create binary document with key $binaryKey for content $contentKey ($versionKey)",
                    true
                )
                return@forEach
            }

            val result = openSearchClient.indexBinary(binaryDocument)

            status.setResult(
                binaryKey,
                CmsElementType.BINARY,
                result,
                " for content $contentKey version $versionKey"
            )
        }
    }
}
