package no.nav.openSearch.documents.category

import CategoryRefData
import no.nav.cms.client.CmsClient
import no.nav.utils.parseDateTime
import no.nav.utils.xmlToString
import org.jdom.Element


class OpenSearchCategoryDocumentBuilder(private val cmsClient: CmsClient) {

    fun build(categoryKey: Int): OpenSearchCategoryDocument? {
        val categoryElement = cmsClient.getCategory(categoryKey, 1) ?: return null
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
            ?.getChildren("category")
            ?.filterIsInstance<Element>()
            ?.map {
                CategoryRefData(
                    key = it.getAttributeValue("key"),
                    name = it.getChildText("title"),
                )
            }
    }

    private fun getContentReferences(categoryKey: Int): List<ContentRefData>? {
        val contentsDocument = cmsClient.getContentByCategory(categoryKey, 1, 0, 1000)

        return contentsDocument
            ?.rootElement
            ?.getChildren("content")
            ?.filterIsInstance<Element>()
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