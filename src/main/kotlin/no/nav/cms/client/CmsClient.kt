package no.nav.cms.client

import com.enonic.cms.api.client.Client
import com.enonic.cms.api.client.ClientFactory
import com.enonic.cms.api.client.model.GetContentParams
import com.enonic.cms.api.client.model.GetMenuParams
import io.ktor.util.logging.*
import org.jdom.Document

val logger = KtorSimpleLogger("CmsClient")

class CmsClient (url: String, userName: String, password: String) {
    private val client: Client

    init {
        client = ClientFactory.getRemoteClient(url)
        client.login(userName, password)

        logger.info("Logged in as ${client.userName}")
    }

    fun getContent(key: Int): Document {
        return getContent(intArrayOf(key))
    }

    fun getContent(keys: IntArray): Document {
        val params = GetContentParams()
        params.contentKeys = keys
        params.includeData = false
        params.includeVersionsInfo = true

        return client.getContent(params)
    }

    fun getMenu(key: Int): Document {
        val params = GetMenuParams()
        params.menuKey = key
        params.includeHidden = true

        return client.getMenu(params)
    }
}
