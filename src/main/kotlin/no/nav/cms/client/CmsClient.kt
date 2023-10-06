package no.nav.cms.client

import com.enonic.cms.api.client.Client
import com.enonic.cms.api.client.ClientException
import com.enonic.cms.api.client.ClientFactory
import com.enonic.cms.api.client.model.GetContentParams
import com.enonic.cms.api.client.model.GetContentVersionsParams
import com.enonic.cms.api.client.model.GetMenuParams
import com.enonic.cms.api.client.model.RenderContentParams
import io.ktor.util.logging.*
import org.jdom.Document

val logger = KtorSimpleLogger("CmsClient")

class CmsClient (url: String) {
    private val client: Client

    init {
        client = ClientFactory.getRemoteClient(url)
    }

    fun login(userName: String, password: String): Boolean {
        return try {
            client.login(userName, password)
            logger.info("Logged in as ${client.userName}")
            true
        } catch (e: ClientException) {
            logger.error("Login failed for user $userName - ${e.message}")
            false
        }
    }

    fun getContent(keys: IntArray): Document {
        val params = GetContentParams()
        params.contentKeys = keys
        params.includeData = false
        params.includeVersionsInfo = true

        return client.getContent(params)
    }

    fun getContent(key: Int): Document {
        return getContent(intArrayOf(key))
    }

    fun getContentVersions(keys: IntArray): Document {
        val params = GetContentVersionsParams()
        params.contentVersionKeys = keys
        params.contentRequiredToBeOnline = false

        return client.getContentVersions(params)
    }

    fun getContentVersion(key: Int): Document {
        return getContent(intArrayOf(key))
    }

    fun getMenu(key: Int): Document {
        val params = GetMenuParams()
        params.menuKey = key
        params.includeHidden = true

        return client.getMenu(params)
    }

    fun renderContent(siteKey: Int, contentKey: Int): Document {
        val params = RenderContentParams()
        params.siteKey = siteKey
        params.contentKey = contentKey
        params.serverName = "asdf"
        params.basePath = "/"

        return client.renderContent(params)
    }
}
