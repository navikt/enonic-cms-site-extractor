package no.nav.extractor

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder


private val logger = KtorSimpleLogger("CmsContentExtractor")

sealed class CmsExtractor(
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    val errors = LinkedHashMap<Int, String>()
    val results = LinkedHashMap<Int, String>()
    var isRunning = false

    fun getErrors(): String {
        return errors.toString()
    }

    fun getResults(): String {
        return results.toString()
    }

    protected fun start(): Boolean {
        if (isRunning) {
            return false
        }

        isRunning = true
        errors.clear()
        results.clear()

        return true
    }

    protected fun stop() {
        isRunning = false
    }

    private fun logError(key: Int, msg: String) {
        errors[key] = msg
        logger.error("[$key]: $msg")
    }

    private fun logResult(key: Int, msg: String) {
        results[key] = msg
        logger.info("[$key]: $msg")
    }

    protected suspend fun extractCategory(
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
                extractContent(it.key.toInt(), withVersions)
            }
        }

        if (withChildren) {
            categoryDocument.categories?.forEach {
                extractCategory(it.key.toInt(), withChildren, withContent, withVersions)
            }
        }
    }

    protected suspend fun extractContent(contentKey: Int, withVersions: Boolean) {
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
                    extractVersion(it.key.toInt())
                }
            }
        }
    }

    protected suspend fun extractVersion(versionKey: Int) {
        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)

        if (contentVersionDocument == null) {
            logError(versionKey, "Failed to create content document with version key $versionKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentVersionDocument)

        logResult(versionKey, "Result for content version $versionKey to ${result.index}: ${result.result}")
    }
}
