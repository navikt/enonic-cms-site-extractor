package no.nav.cms.renderer

import io.ktor.util.logging.*
import no.nav.cms.client.CmsRestClient
import no.nav.cms.client.CmsRpcClient
import org.jdom.Element
import java.lang.Exception

private const val CONTENT_ELEMENT_NAME = "content"


private val logger = KtorSimpleLogger("ContentRenderer")

class ContentRenderer(contentKey: Int, rpcClient: CmsRpcClient, restClient: CmsRestClient) {
    private val rpcClient: CmsRpcClient
    private val restClient: CmsRestClient

    private val contentElement: Element

    init {
        this.rpcClient = rpcClient
        this.restClient = restClient

        val document = rpcClient.getContent(contentKey)

        val rootElement = document.rootElement
        val contentElement = rootElement.getChild("content")

        if (contentElement.name != CONTENT_ELEMENT_NAME) {
            throw Exception("Element is not a valid content element")
        }

        this.contentElement = contentElement
    }

    suspend fun render(): String? {
        val params = ContentRenderParamsBuilder(this.contentElement, this.restClient).build()

        if (params == null) {
            return null
        }

        return this.restClient.renderPage(params)
    }
}