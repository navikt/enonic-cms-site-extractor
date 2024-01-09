package no.nav.migration

import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocumentBuilder
import no.nav.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.OpenSearchContentDocumentBuilder


private val logger = KtorSimpleLogger("CmsContentMigrator")

enum class CmsMigratorState {
    NOT_STARTED, RUNNING, ABORTED, FAILED, FINISHED
}

@Serializable
data class CmsMigratorStatus(
    val state: CmsMigratorState,
    val results: MutableList<String>,
    val errors: MutableList<String>,
)

class CmsMigrator(
    val params: CmsMigratorParams,
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    private var job: Job? = null

    private var state = CmsMigratorState.NOT_STARTED

    private val results = mutableListOf<String>()
    private val errors = mutableListOf<String>()

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun run(): String {
        if (state == CmsMigratorState.RUNNING) {
            return "Migrator for ${params.key} is already running"
        }

        job = GlobalScope.launch {
            runJob()
        }

        val msg = when (params) {
            is CmsCategoryMigratorParams -> {
                "Started migrating category ${params.key} " +
                        "(with children: ${params.withChildren} - " +
                        "with content: ${params.withContent} - " +
                        "with versions: ${params.withVersions})"
            }

            is CmsContentMigratorParams -> {
                "Started migrating content ${params.key} " +
                        "(with versions: ${params.withVersions})"
            }

            is CmsVersionMigratorParams -> {
                "Started migrating version ${params.key}"
            }
        }

        logger.info(msg)
        return msg
    }

    private suspend fun runJob() {
        state = CmsMigratorState.RUNNING

        errors.clear()
        results.clear()

        try {
            when (params) {
                is CmsCategoryMigratorParams -> {
                    migrateCategory(
                        params.key,
                        params.withChildren ?: false,
                        params.withContent ?: false,
                        params.withVersions ?: false
                    )
                }

                is CmsContentMigratorParams -> {
                    migrateContent(
                        params.key,
                        params.withVersions ?: false
                    )
                }

                is CmsVersionMigratorParams -> {
                    migrateVersion(params.key)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                logger.info("Job for ${params.key} was cancelled")
            } else {
                logger.error("Exception while running job for ${params.key} - ${e.message}")
                state = CmsMigratorState.FAILED
            }
            throw e
        }

        logger.info("Finished running job for ${params.key}")
        state = CmsMigratorState.FINISHED
    }

    suspend fun abort() {
        state = CmsMigratorState.ABORTED
        logger.info("Sending cancel signal for ${params.key}")
        job?.cancelAndJoin()
    }

    fun getStatus(): CmsMigratorStatus {
        return CmsMigratorStatus(state, results, errors)
    }

    private fun logError(msg: String) {
        errors.add(msg)
        logger.error(msg)
    }

    private fun logResult(msg: String) {
        results.add(msg)
        logger.info(msg)
    }

    private suspend fun migrateCategory(
        categoryKey: Int,
        withChildren: Boolean,
        withContent: Boolean,
        withVersions: Boolean
    ) {
        val categoryDocument = OpenSearchCategoryDocumentBuilder(cmsClient).build(categoryKey)

        if (categoryDocument == null) {
            logError("Failed to create category document for $categoryKey")
            return
        }

        val result = openSearchClient.indexCategoryDocument(categoryDocument)

        logResult("Result for category $categoryKey to ${result.index}: ${result.result}")

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

    private suspend fun migrateContent(contentKey: Int, withVersions: Boolean) {
        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(contentKey)

        if (contentDocument == null) {
            logError("Failed to create content document with content key $contentKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentDocument)

        logResult("Result for content $contentKey to ${result.index}: ${result.result}")

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
            logError(
                "Failed to create content document with version key $versionKey"
            )
            return
        }

        val result = openSearchClient.indexContentDocument(contentVersionDocument)

        logResult("Result for content version $versionKey to ${result.index}: ${result.result}")

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

            if (binaryDocument == null) {
                logError(
                    "Failed to create binary document with key $binaryKey for content $contentKey ($versionKey)"
                )
                return
            }

            val result = openSearchClient.indexBinaryDocument(binaryDocument)

            logResult(
                "Result for binary with key $binaryKey for content $contentKey ($versionKey): ${result.result}"
            )
        }
    }
}
