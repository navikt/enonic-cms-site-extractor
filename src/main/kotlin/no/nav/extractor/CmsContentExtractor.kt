package no.nav.extractor

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder


private val logger = KtorSimpleLogger("CmsContentExtractor")

class CmsContentExtractor(
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient,
) {
    private val errors = LinkedHashMap<Int, String>()
    private val results = LinkedHashMap<Int, String>()

    private var isRunning = false

    suspend fun runExtractCategory(
        categoryKey: Int,
        withChildren: Boolean?,
        withContent: Boolean?,
        withVersions: Boolean?
    ) {
        if (start()) {
            extractCategory(
                categoryKey,
                withChildren ?: false,
                withContent ?: false,
                withVersions ?: false
            )

            stop()
        }
    }

    suspend fun runExtractContent(contentKey: Int, withVersions: Boolean?) {
        if (start()) {
            extractContent(contentKey, withVersions ?: false)

            stop()
        }
    }

    suspend fun runExtractVersion(versionKey: Int) {
        if (start()) {
            extractVersion(versionKey)

            stop()
        }
    }

    fun getErrors(): String {
        return errors.toString()
    }

    fun getResults(): String {
        return results.toString()
    }

    private fun start(): Boolean {
        if (isRunning) {
            return false
        }

        isRunning = true
        errors.clear()
        results.clear()

        return true
    }

    private fun stop() {
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

    private suspend fun extractCategory(
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

    private suspend fun extractContent(contentKey: Int, withVersions: Boolean) {
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

    private suspend fun extractVersion(versionKey: Int) {
        val contentVersionDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)

        if (contentVersionDocument == null) {
            logError(versionKey, "Failed to create content document with version key $versionKey")
            return
        }

        val result = openSearchClient.indexContentDocument(contentVersionDocument)

        logResult(versionKey, "Result for content version $versionKey to ${result.index}: ${result.result}")
    }
}
