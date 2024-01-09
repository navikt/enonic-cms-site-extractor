package no.nav.openSearch.documents.content

import CategoryRefData
import no.nav.openSearch.documents._partials.cmsUser.CmsUserData
import no.nav.cms.client.CmsClient
import no.nav.cms.utils.getContentElement
import no.nav.utils.parseDateTime
import no.nav.utils.xmlToString
import org.jdom.Document
import org.jdom.Element


class OpenSearchContentDocumentBuilder(private val cmsClient: CmsClient) {

    suspend fun buildDocumentFromContent(contentKey: Int): OpenSearchContentDocument? {
        val document = cmsClient.getContent(contentKey)
        return transform(document)
    }

    suspend fun buildDocumentFromVersion(versionKey: Int): OpenSearchContentDocument? {
        val document = cmsClient.getContentVersion(versionKey)
        return transform(document)
    }

    private suspend fun transform(cmsDocument: Document?): OpenSearchContentDocument? {
        if (cmsDocument == null) {
            return null
        }

        val contentElement = getContentElement(cmsDocument) ?: return null
        val documentXml = xmlToString(contentElement.document) ?: return null
        val html = this.cmsClient.renderDocument(cmsDocument)

        return OpenSearchContentDocument(
            contentKey = contentElement.getAttributeValue("key"),
            versionKey = contentElement.getAttributeValue("versionkey"),
            isCurrentVersion = contentElement.getAttribute("current").booleanValue,
            name = contentElement.getChildText("name"),
            displayName = contentElement.getChildText("display-name"),
            versions = getVersionReferences(contentElement),
            locations = getLocations(contentElement),
            category = getCategory(contentElement),
            binaries = getBinaryReferences(contentElement),
            meta = getMetaData(contentElement),
            html = html,
            xmlAsString = documentXml,
        )
    }

    private fun getMetaData(element: Element): ContentMetaData {
        return ContentMetaData(
            unitKey = element.getAttributeValue("unitkey"),
            state = element.getAttributeValue("state"),
            status = element.getAttributeValue("status"),
            published = parseDateTime(element.getAttributeValue("published")),
            languageCode = element.getAttributeValue("languagecode"),
            languageKey = element.getAttributeValue("languagekey"),
            priority = element.getAttributeValue("priority"),
            contentType = element.getAttributeValue("contenttype"),
            contentTypeKey = element.getAttributeValue("contenttypekey"),
            created = parseDateTime(element.getAttributeValue("created")),
            timestamp = parseDateTime(element.getAttributeValue("timestamp")),
            publishFrom = parseDateTime(element.getAttributeValue("publishfrom")),
            publishTo = parseDateTime(element.getAttributeValue("publishto")),
            owner = transformToCmsUser(element.getChild("owner")),
            modifier = transformToCmsUser(element.getChild("modifier")),
        )
    }

    private fun getCategory(element: Element): CategoryRefData {
        val category = element.getChild("category")

        return CategoryRefData(
            key = category.getAttributeValue("key"),
            name = category.getAttributeValue("name")
        )
    }

    private fun getLocations(element: Element): List<ContentLocation>? {
        return element
            .getChild("location")
            ?.getChildren("site")
            ?.filterIsInstance<Element>()
            ?.flatMap { site ->
                val siteKey = site.getAttributeValue("key")

                site.getChildren("contentlocation").filterIsInstance<Element>().map { location ->
                    ContentLocation(
                        siteKey = siteKey,
                        type = location.getAttributeValue("type"),
                        menuItemKey = location.getAttributeValue("menuitemkey"),
                        menuItemName = location.getAttributeValue("menuitemname"),
                        menuItemPath = location.getAttributeValue("menuitempath"),
                        menuItemDisplayName = location.getAttributeValue("menuitem-display-name"),
                        home = location.getAttribute("home").booleanValue,
                    )
                }
            }
    }

    private fun getVersionReferences(element: Element): List<ContentVersionReference>? {
        return element
            .getChild("versions")
            ?.getChildren("version")
            ?.filterIsInstance<Element>()
            ?.map {
                ContentVersionReference(
                    key = it.getAttributeValue("key"),
                    statusKey = it.getAttributeValue("status-key"),
                    status = it.getAttributeValue("status"),
                    timestamp = parseDateTime(it.getAttributeValue("timestamp")),
                    title = it.getChildText("title"),
                    comment = it.getChildText("comment"),
                    modifier = transformToCmsUser(it.getChild("modifier"))
                )
            }
    }

    private fun getBinaryReferences(element: Element): List<ContentBinaryReference>? {
        return element
            .getChild("binaries")
            ?.getChildren("binary")
            ?.filterIsInstance<Element>()
            ?.map {
                ContentBinaryReference(
                    key = it.getAttributeValue("key"),
                    filename = it.getAttributeValue("filename"),
                    filesize = it.getAttributeValue("filesize").toInt()
                )
            }
    }

    private fun transformToCmsUser(element: Element): CmsUserData {
        return CmsUserData(
            userstore = element.getChildText("userstore"),
            name = element.getChildText("name"),
            displayName = element.getChildText("display-name"),
            email = element.getChildText("email")
        )
    }
}