package no.nav.cms.client

import com.enonic.cms.api.client.ClientException
import com.enonic.cms.api.client.ClientFactory
import com.enonic.cms.api.client.RemoteClient
import com.enonic.cms.api.client.model.GetCategoriesParams
import com.enonic.cms.api.client.model.GetContentByCategoryParams
import com.enonic.cms.api.client.model.GetContentParams
import com.enonic.cms.api.client.model.GetContentVersionsParams
import com.enonic.cms.api.client.model.GetMenuDataParams
import com.enonic.cms.api.client.model.GetMenuItemParams
import com.enonic.cms.api.client.model.GetMenuParams
import io.ktor.server.auth.*
import io.ktor.util.logging.*
import no.nav.cms.renderer.ContentRenderParams
import org.jdom.Document


private const val RPC_PATH = "/rpc/bin"

private val logger = KtorSimpleLogger("CmsClient")

class CmsClient(cmsOrigin: String, credential: UserPasswordCredential) {
    private val cmsOrigin: String
    private val rpcClient: RemoteClient
    private val restClient: CmsRestClient

    init {
        this.cmsOrigin = cmsOrigin
        this.rpcClient = ClientFactory.getRemoteClient(cmsOrigin.plus(RPC_PATH))
        this.restClient = CmsRestClient(cmsOrigin, credential)
    }

    fun login(username: String, password: String): Boolean {
        return try {
            rpcClient.login(username, password)
            logger.info("Logged in as ${rpcClient.userName}")
            true
        } catch (e: ClientException) {
            logger.error("Login failed for user $username - ${e.message}")
            false
        }
    }

    fun getContent(keys: IntArray): Document {
        val params = GetContentParams()
        params.contentKeys = keys
        params.includeData = false
        params.includeVersionsInfo = true
        params.includeOfflineContent = true

        return rpcClient.getContent(params)
    }

    fun getContent(key: Int): Document {
        return getContent(intArrayOf(key))
    }

    fun getContentVersions(keys: IntArray): Document {
        val params = GetContentVersionsParams()
        params.contentVersionKeys = keys
        params.contentRequiredToBeOnline = false

        return rpcClient.getContentVersions(params)
    }

    fun getContentVersion(key: Int): Document {
        return getContentVersions(intArrayOf(key))
    }

    fun getMenu(key: Int): Document {
        val params = GetMenuParams()
        params.menuKey = key
        params.includeHidden = true

        return rpcClient.getMenu(params)
    }

    fun getMenuItem(key: Int): Document {
        val params = GetMenuItemParams()
        params.menuItemKey = key
        params.details = true

        return rpcClient.getMenuItem(params)
    }

    fun getCategories(key: Int, depth: Int?): Document {
        val params = GetCategoriesParams()
        params.categoryKey = key
        params.includeTopCategory = true
        params.levels = depth ?: 0

        return rpcClient.getCategories(params)
    }

    fun getContentByCategory(key: Int): Document {
        val params = GetContentByCategoryParams()
        params.categoryKeys = intArrayOf(key)
        params.includeOfflineContent = true
        params.includeData = false

        return rpcClient.getContentByCategory(params)
    }

    fun getMenuData(key: Int): Document {
        val params = GetMenuDataParams()
        params.menuKey = key

        return rpcClient.getMenuData(params)
    }

    suspend fun getPageTemplateKey(contentKey: String, versionKey: String, pageKey: String, unitKey: String): String? {
        return restClient.getPageTemplateKey(contentKey, versionKey, pageKey, unitKey)
    }

    suspend fun renderContent(params: ContentRenderParams): String? {
        return restClient.renderContent(params)
    }
}
