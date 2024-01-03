package no.nav.extractor

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder


private val logger = KtorSimpleLogger("CmsContentExtractor")

class CmsContentExtractor(cmsClient: CmsClient, openSearchClient: OpenSearchClient) {
    private val cmsClient: CmsClient = cmsClient
    private val openSearchClient: OpenSearchClient = openSearchClient

    suspend fun extractCategoryToOpenSearch(
        categoryKey: Int,
        withContent: Boolean? = false,
        withChildren: Boolean? = false
    ): String {
        val categoryDocument = OpenSearchCategoryDocumentBuilder(cmsClient).build(categoryKey)
            ?: return "No category found with key $categoryKey"

        val result = openSearchClient.indexCategoryDocument(categoryDocument)

        if (withContent == true) {
            categoryDocument.contents?.forEach {
                val resultMsg = extractContentToOpenSearch(it.key.toInt())
                logger.info(resultMsg)
            }
        }

        if (withChildren == true) {
            categoryDocument.categories?.forEach {
                val resultMsg = extractCategoryToOpenSearch(it.key.toInt(), withContent, withChildren)
                logger.info(resultMsg)
            }
        }

        return "Result for category $categoryKey to ${result.index}: ${result.result}"
    }

    suspend fun extractContentToOpenSearch(contentKey: Int, withVersions: Boolean? = false): String {
        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(contentKey)
            ?: return "No content found with key $contentKey"


        val result = openSearchClient.indexContentDocument(contentDocument)

        if (withVersions == true) {
            contentDocument.versions.forEach {
                if (it.key != contentDocument.versionKey) {
                    val resultMsg = extractContentVersionToOpenSearch(it.key.toInt())
                    logger.info(resultMsg)
                }
            }
        }

        return "Result for content $contentKey to ${result.index}: ${result.result}"
    }

    suspend fun extractContentVersionToOpenSearch(versionKey: Int): String {
        val contentDocument = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(versionKey)
            ?: return "No content found with version key $versionKey"


        val result = openSearchClient.indexContentDocument(contentDocument)

        return "Result for content version $versionKey to ${result.index}: ${result.result}"
    }
}
