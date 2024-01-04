package no.nav.cms.renderer

import no.nav.cms.utils.getContentElement
import no.nav.cms.client.CmsClient
import org.jdom.Document


class ContentRenderer(private val cmsClient: CmsClient) {

    suspend fun renderDocument(document: Document): String? {
        val contentElement = getContentElement(document) ?: return null
        val params = ContentRenderParamsBuilder(contentElement, cmsClient).build() ?: return null

        return cmsClient.renderContent(params)
    }

    suspend fun renderVersion(versionKey: Int): String? {
        return renderDocument(cmsClient.getContentVersion(versionKey))
    }

    suspend fun renderContent(contentKey: Int): String? {
        return renderDocument(cmsClient.getContent(contentKey))
    }
}
