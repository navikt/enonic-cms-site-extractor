package no.nav.db.openSearch.documents.category

import CategoryRefData
import no.nav.cms.client.CmsClient
import no.nav.utils.xmlToString
import org.jdom.Element


class OpenSearchCategoryDocumentBuilder(private val cmsClient: CmsClient) {

    fun buildDocument(categoryKey: Int): OpenSearchCategoryDocument? {
        val categoryElement = cmsClient.getCategory(categoryKey, 1) ?: return null

        return transform(categoryElement)
    }

    private fun transform(categoryElement: Element): OpenSearchCategoryDocument? {
        val xmlString = xmlToString(categoryElement) ?: return null

        return OpenSearchCategoryDocument(
            xmlAsString = xmlString,
            key = categoryElement.getAttributeValue("key"),
            title = categoryElement.getChildText("title"),
            contentTypeKey = categoryElement.getAttributeValue("contenttypekey"),
            superKey = categoryElement.getAttributeValue("superkey"),
            categories = getCategoryReferences(categoryElement),
        )
    }

    private fun getCategoryReferences(element: Element): List<CategoryRefData> {
        return element
            .getChild("categories")
            .getChildren("category")
            .filterIsInstance<Element>()
            .map {
                CategoryRefData(
                    key = it.getAttributeValue("key"),
                    name = it.getChildText("title"),
                )
            }
    }
}