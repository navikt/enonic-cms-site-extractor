package no.nav.openSearch.documents.category

import CategoryRefData
import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.cms.utils.getCategoryElement
import no.nav.cms.utils.getChildElements
import no.nav.utils.parseDateTime
import no.nav.utils.xmlToString
import org.jdom.Element


private val logger = KtorSimpleLogger("OpenSearchCategoryDocumentBuilder")

class OpenSearchCategoryDocumentBuilder(private val cmsClient: CmsClient) {

    fun build(categoryKey: Int): OpenSearchCategoryDocument? {
        val categoryDocument = cmsClient.getCategory(categoryKey, 1)

        if (categoryDocument == null) {
            logger.info("Category document not found for $categoryKey")
            return null
        }

        val categoryElement = categoryDocument
            .run { getCategoryElement(this) }

        if (categoryElement == null) {
            logger.info("Category element not found for $categoryKey - Document: ${xmlToString(categoryDocument)}")
            return null
        }

        val xmlString = xmlToString(categoryElement) ?: return null

        return OpenSearchCategoryDocument(
            xmlAsString = xmlString,
            key = categoryElement.getAttributeValue("key"),
            title = categoryElement.getChildText("title"),
            contentTypeKey = categoryElement.getAttributeValue("contenttypekey"),
            superKey = categoryElement.getAttributeValue("superkey"),
            categories = getCategoryReferences(categoryElement),
            contents = getContentReferences(categoryKey),
        )
    }

    private fun getCategoryReferences(element: Element): List<CategoryRefData>? {
        return element
            .getChild("categories")
            ?.run { getChildElements(this, "category") }
            ?.map {
                CategoryRefData(
                    key = it.getAttributeValue("key"),
                    name = it.getChildText("title"),
                )
            }
    }

    private fun getContentReferences(categoryKey: Int): List<ContentRefData>? {
        val contentsDocument = cmsClient.getContentByCategory(categoryKey, 1)

        return contentsDocument
            ?.rootElement
            ?.run { getChildElements(this, "content") }
            ?.map {
                ContentRefData(
                    key = it.getAttributeValue("key"),
                    name = it.getChildText("name"),
                    displayName = it.getChildText("display-name"),
                    timestamp = parseDateTime(it.getAttributeValue("timestamp")),
                )
            }
    }
}