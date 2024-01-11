package no.nav.migration

import kotlinx.coroutines.*
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocumentBuilder
import no.nav.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.OpenSearchContentDocumentBuilder


private enum class State {
    NOT_STARTED, RUNNING, ABORTED, FAILED, FINISHED
}

private val finishedStates = listOf(State.FINISHED, State.FAILED, State.ABORTED)

class CmsMigrator(
    private val params: ICmsMigrationParams,
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    private var state: State = State.NOT_STARTED
    private var job: Job? = null

    private val status: CmsMigrationStatus = CmsMigrationStatus(
        params,
        cmsClient,
        openSearchClient
    )

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run() {
        if (state != State.NOT_STARTED) {
            status.log("Attempted to start migration for ${params.key}, but it was already started - current state: $state")
            return
        }

        status.log("Starting migration job for ${params.key}")

        job = GlobalScope.launch {
            runJob()
        }
    }

    private suspend fun setState(newState: State) {
        status.log("Setting migration state to $newState")
        state = newState

        if (newState in finishedStates) {
            status.persistToDb()
        }
    }

    private suspend fun runJob() {
        setState(State.RUNNING)

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
                setState(State.ABORTED)
            } else {
                status.log("Exception while running job for ${params.key} - ${e.message}", true)
                setState(State.FAILED)
            }
            throw e
        }

        setState(State.FINISHED)
    }

    suspend fun abort() {
        status.log("Sending cancel signal for ${params.key}")
        job?.cancelAndJoin()
    }

    fun getStatus(withResults: Boolean?, withRemaining: Boolean?): CmsMigrationStatusData {
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
            val binaryDocument = OpenSearchBinaryDocumentBuilder(cmsClient)
                .build(
                    it,
                    contentKey = contentKey,
                    versionKey = versionKey
                )

            val binaryKey = it.key.toInt()

            if (binaryDocument != null) {
                val result = openSearchClient.indexBinary(binaryDocument)

                status.setResult(
                    binaryKey,
                    CmsElementType.BINARY,
                    result,
                    " for content $contentKey version $versionKey"
                )
            } else {
                status.log(
                    "Failed to create binary document with key $binaryKey for content $contentKey ($versionKey)",
                    true
                )
            }
        }
    }
}
