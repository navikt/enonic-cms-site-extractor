package no.nav.migration

import CmsMigrationStatusSummary
import no.nav.cms.client.CmsClient
import no.nav.migration.status.CmsMigrationStatus
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocumentBuilder
import no.nav.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.OpenSearchContentDocumentBuilder


enum class CmsMigratorState {
    READY, RUNNING, ABORTED, FAILED, FINISHED
}

private val finishedStates = setOf(CmsMigratorState.FINISHED, CmsMigratorState.FAILED, CmsMigratorState.ABORTED)

private class AbortedException : Exception()

class CmsMigrator(
    private val status: CmsMigrationStatus,
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    private var state: CmsMigratorState = CmsMigratorState.READY
    val jobId = status.data.jobId
    val baseKey = status.data.params.key

    suspend fun run() {
        if (state != CmsMigratorState.READY) {
            status.log("Attempted to start migration job ${status.data.jobId}, but it was already started - current state: $state")
            return
        }

        status.log("Starting migration job ${status.data.jobId}")

        runJob()
    }

    suspend fun prepareRetry() {
        if (state == CmsMigratorState.RUNNING) {
            status.log("Attempted to retry job, but it was already running")
            return
        }

        status.log("Preparing to retry job")

        setState(CmsMigratorState.READY)
    }

    private suspend fun runJob() {
        setState(CmsMigratorState.RUNNING)

        val categories = status.data.remaining.categories.toList()
        val contents = status.data.remaining.contents.toList()
        val versions = status.data.remaining.versions.toList()

        try {
            categories.forEach { migrateCategory(it) }
            contents.forEach { migrateContent(it) }
            versions.forEach { migrateVersion(it) }
        } catch (e: Exception) {
            if (e is AbortedException) {
                status.log("Job ${status.data.jobId} was aborted")
                return
            } else {
                status.log("Exception while running job ${status.data.jobId} - ${e.message}", true)
                setState(CmsMigratorState.FAILED)
                throw e
            }
        }

        setState(CmsMigratorState.FINISHED)
    }

    fun getState(): CmsMigratorState {
        return state
    }

    private suspend fun setState(newState: CmsMigratorState) {
        status.log("Setting migration state to $newState")
        state = newState

        if (newState in finishedStates) {
            status.finish()
        }
    }

    suspend fun abort() {
        setState(CmsMigratorState.ABORTED)
    }

    private fun handleAborted() {
        if (state == CmsMigratorState.ABORTED) {
            throw AbortedException()
        }
    }

    fun getStatusSummary(): CmsMigrationStatusSummary {
        return status.getStatusSummary()
    }

    private suspend fun migrateCategory(categoryKey: Int) {
        handleAborted()

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
    }

    private suspend fun migrateContent(contentKey: Int) {
        handleAborted()

        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildFromContent(contentKey)

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
    }

    private suspend fun migrateVersion(versionKey: Int) {
        handleAborted()

        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildFromVersion(versionKey)

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
            handleAborted()

            val binaryKey = it.key.toInt()

            if (status.data.migrated.binaries.contains(binaryKey)) {
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
