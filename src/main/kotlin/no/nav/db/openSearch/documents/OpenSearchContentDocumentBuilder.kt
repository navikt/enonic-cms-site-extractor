package no.nav.db.openSearch.documents

import no.nav.cms.client.CmsClient
import no.nav.cms.renderer.ContentRenderer
import no.nav.cms.utils.getContentElement
import no.nav.utils.documentToString
import org.jdom.Document
import org.jdom.Element
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class OpenSearchContentDocumentBuilder(cmsClient: CmsClient) {
    private val cmsClient: CmsClient

    init {
        this.cmsClient = cmsClient
    }

    suspend fun buildDocumentFromContent(contentKey: Int): OpenSearchContentDocument? {
        val document = cmsClient.getContent(contentKey)
        return transform(document)
    }

    suspend fun buildDocumentFromVersion(versionKey: Int): OpenSearchContentDocument? {
        val document = cmsClient.getContentVersion(versionKey)
        return transform(document)
    }

    private suspend fun transform(cmsDocument: Document): OpenSearchContentDocument? {
        val contentElement = getContentElement(cmsDocument) ?: return null

        val documentXml = documentToString(contentElement.document) ?: return null

        val html = ContentRenderer(this.cmsClient).renderDocument(cmsDocument)


        return OpenSearchContentDocument(
            path = "",
            contentKey = contentElement.getAttributeValue("key"),
            versionKey = contentElement.getAttributeValue("versionkey"),
            isCurrentVersion = contentElement.getAttribute("current").booleanValue,
            name = contentElement.getChildText("name"),
            displayName = contentElement.getChildText("display-name"),
            versions = getVersionReferences(contentElement),
            locations = getLocations(contentElement),
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
            category = getCategory(element),
            owner = transformToCmsUser(element.getChild("owner")),
            modifier = transformToCmsUser(element.getChild("modifier")),
        )
    }

    private fun getCategory(element: Element): ContentCategory {
        val category = element.getChild("category")

        return ContentCategory(
            key = category.getAttributeValue("key"),
            name = category.getAttributeValue("name")
        )
    }

    private fun getLocations(element: Element): List<ContentLocation> {
        return element
            .getChild("location")
            .getChildren("site")
            .filterIsInstance<Element>()
            .flatMap { site ->
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

    private fun getVersionReferences(element: Element): List<ContentVersionReference> {
        return element
            .getChild("versions")
            .getChildren("version")
            .filterIsInstance<Element>()
            .map {
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

    private fun transformToCmsUser(element: Element): CmsUser {
        return CmsUser(
            userstore = element.getChildText("userstore"),
            name = element.getChildText("name"),
            displayName = element.getChildText("display-name"),
            email = element.getChildText("email")
        )
    }

    private fun parseDateTime(datetime: String?): String? {
        if (datetime == null) {
            return null
        }

        return try {
            LocalDateTime
                .parse(datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss][.S][S][S]"))
                .toString()
        } catch (e: DateTimeParseException) {
            return null
        }
    }
}