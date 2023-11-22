package no.nav.cms.renderer

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import org.jdom.Element
import java.lang.Exception


private const val CONTENT_ELEMENT_NAME = "content"

private val logger = KtorSimpleLogger("ContentRenderer")

class ContentRenderer(contentKey: Int, cmsClient: CmsClient) {
    private val cmsClient: CmsClient
    private val contentElement: Element

    init {
        this.cmsClient = cmsClient

        val document = cmsClient.getContent(contentKey)

        val rootElement = document.rootElement
        val contentElement = rootElement.getChild("content")

        if (contentElement.name != CONTENT_ELEMENT_NAME) {
            throw Exception("Element is not a valid content element")
        }

        this.contentElement = contentElement
    }

    suspend fun render(): String? {
        val params = ContentRenderParamsBuilder(this.contentElement, this.cmsClient).build() ?: return null

        return this.cmsClient.renderContent(params)
    }
}
