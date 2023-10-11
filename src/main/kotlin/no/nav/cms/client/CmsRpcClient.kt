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
import com.enonic.cms.api.client.model.GetResourceParams
import com.enonic.cms.api.client.model.RenderContentParams
import io.ktor.util.logging.*
import org.jdom.Document


private const val RPC_PATH = "/rpc/bin"

private val logger = KtorSimpleLogger("CmsClient")

class CmsRpcClient(cmsOrigin: String) {
    private val client: RemoteClient
    private val cmsOrigin: String

    init {
        this.cmsOrigin = cmsOrigin
        this.client = ClientFactory.getRemoteClient("$cmsOrigin$RPC_PATH")
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
        params.includeOfflineContent = true

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
        return getContentVersions(intArrayOf(key))
    }

    fun getMenu(key: Int): Document {
        val params = GetMenuParams()
        params.menuKey = key
        params.includeHidden = true

        return client.getMenu(params)
    }

    fun getMenuItem(key: Int): Document {
        val params = GetMenuItemParams()
        params.menuItemKey = key
        params.details = true

        return client.getMenuItem(params)
    }

    fun getCategories(key: Int, depth: Int?): Document {
        val params = GetCategoriesParams()
        params.categoryKey = key
        params.includeTopCategory = true
        params.levels = depth ?: 0

        return client.getCategories(params)
    }

    fun getContentByCategory(key: Int): Document {
        val params = GetContentByCategoryParams()
        params.categoryKeys = intArrayOf(key)
        params.includeOfflineContent = true
        params.includeData = false

        return client.getContentByCategory(params)
    }

    fun getMenuData(key: Int): Document {
        val params = GetMenuDataParams()
        params.menuKey = key

        return client.getMenuData(params)
    }

    fun renderContent(siteKey: Int, contentKey: Int): Document {
        val params = RenderContentParams()
        params.siteKey = siteKey
        params.contentKey = contentKey
        params.serverName = this.cmsOrigin
        params.basePath = "/"

        return client.renderContent(params)
    }
}
