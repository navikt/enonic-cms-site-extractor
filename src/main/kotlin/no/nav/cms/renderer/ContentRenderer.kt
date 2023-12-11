package no.nav.cms.renderer

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import org.jdom.Document
import org.jdom.Element
import java.lang.Exception


private const val CONTENT_ELEMENT_NAME = "content"

private val logger = KtorSimpleLogger("ContentRenderer")

class ContentRenderer(cmsClient: CmsClient) {
    private val cmsClient: CmsClient

    init {
        this.cmsClient = cmsClient
    }

    private fun getContentElement(document: Document): Element {
        val contentElement = document
            .rootElement
            .getChild("content")

        if (contentElement.name != CONTENT_ELEMENT_NAME) {
            throw Exception("Element is not a valid content element (expected $CONTENT_ELEMENT_NAME - got ${contentElement.name}")
        }

        return contentElement
    }

    private suspend fun render(document: Document): String? {
        val contentElement = getContentElement(document)

        val params = ContentRenderParamsBuilder(contentElement, cmsClient).build() ?: return null

        return cmsClient.renderContent(params)
    }

    suspend fun renderVersion(versionKey: Int): String? {
        return render(cmsClient.getContentVersion(versionKey))
    }

    suspend fun renderContent(contentKey: Int): String? {
        return render(cmsClient.getContent(contentKey))
    }
}
