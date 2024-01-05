package no.nav.migration

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder


enum class CmsMigratorState {
    NOT_STARTED, RUNNING, ABORTED, FINISHED
}

private val logger = KtorSimpleLogger("CmsContentMigrator")

class CmsMigrator(
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
    val params: CmsMigratorParams
) {
    val errors = LinkedHashMap<Int, String>()
    val results = LinkedHashMap<Int, String>()
    var state = CmsMigratorState.NOT_STARTED

    suspend fun run(): String {
        if (state == CmsMigratorState.RUNNING) {
            return "Migrator for ${params.key} is already running"
        }

        state = CmsMigratorState.RUNNING
        errors.clear()
        results.clear()

        val response = when (params) {
            is CmsCategoryMigratorParams -> {
                migrateCategory(
                    params.key,
                    params.withChildren ?: false,
                    params.withContent ?: false,
                    params.withVersions ?: false
                )
                "Started migrating category ${params.key} " +
                        "(with children: ${params.withChildren} - " +
                        "with content: ${params.withContent} - " +
                        "with versions: ${params.withVersions})"
            }

            is CmsContentMigratorParams -> {
                migrateContent(
                    params.key,
                    params.withVersions ?: false
                )
                "Started migrating content ${params.key} " +
                        "(with versions: ${params.withVersions})"
            }

            is CmsVersionMigratorParams -> {
                migrateVersion(params.key)
                "Started migrating version ${params.key}"
            }
        }

        return response
    }

    fun getErrors(): String {
        return errors.toString()
    }

    fun getResults(): String {
        return results.toString()
    }

    private fun logError(key: Int, msg: String) {
        errors[key] = msg
        logger.error("[$key]: $msg")
    }

    private fun logResult(key: Int, msg: String) {
        results[key] = msg
        logger.info("[$key]: $msg")
    }

    private suspend fun migrateCategory(
        categoryKey: Int,
        withChildren: Boolean,
        withContent: Boolean,
        withVersions: Boolean
    ) {
        val categoryDocument = OpenSearchCategoryDocumentBuilder(cmsClient).build(categoryKey)

        if (categoryDocument == null) {
            logError(categoryKey, "Failed to create category document for $categoryKey")
            return
        }

        val result = openSearchClient.indexCategoryDocument(categoryDocument)

        logResult(categoryKey, "Result for category $categoryKey to ${result.index}: ${result.result}")

        if (withContent) {
            categoryDocument.contents?.forEach {
                migrateContent(it.key.toInt(), withVersions)
            }
        }

        if (withChildren) {
            categoryDocument.categories?.forEach {
                migrateCategory(it.key.toInt(), withChildren, withContent, withVersions)
            }
        }
    }

    private suspend fun migrateContent(contentKey: Int, withVersions: Boolean) {
        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(contentKey)

        if (contentDocument == null) {
            logError(contentKey, "Failed to create content document with content key $contentKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentDocument)

        logResult(contentKey, "Result for content $contentKey to ${result.index}: ${result.result}")

        if (withVersions) {
            contentDocument.versions.forEach {
                if (it.key != contentDocument.versionKey) {
                    migrateVersion(it.key.toInt())
                }
            }
        }
    }

    private suspend fun migrateVersion(versionKey: Int) {
        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)

        if (contentVersionDocument == null) {
            logError(versionKey, "Failed to create content document with version key $versionKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentVersionDocument)

        logResult(versionKey, "Result for content version $versionKey to ${result.index}: ${result.result}")
    }
}
