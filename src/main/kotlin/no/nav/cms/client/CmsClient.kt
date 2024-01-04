package no.nav.cms.client

import com.enonic.cms.api.client.ClientException
import com.enonic.cms.api.client.ClientFactory
import com.enonic.cms.api.client.RemoteClient
import com.enonic.cms.api.client.model.GetCategoriesParams
import com.enonic.cms.api.client.model.GetContentByCategoryParams
import com.enonic.cms.api.client.model.GetContentByQueryParams
import com.enonic.cms.api.client.model.GetContentParams
import com.enonic.cms.api.client.model.GetContentVersionsParams
import com.enonic.cms.api.client.model.GetMenuDataParams
import com.enonic.cms.api.client.model.GetMenuItemParams
import com.enonic.cms.api.client.model.GetMenuParams
import io.ktor.server.auth.*
import io.ktor.util.logging.*
import no.nav.cms.utils.getContentElement
import org.jdom.Document
import org.jdom.Element


private const val RPC_PATH = "/rpc/bin"

private val logger = KtorSimpleLogger("CmsClient")

class CmsClient(cmsOrigin: String, private val credential: UserPasswordCredential) {
    private val rpcClient: RemoteClient = ClientFactory.getRemoteClient(cmsOrigin.plus(RPC_PATH))
    private val restClient: CmsRestClient = CmsRestClient(cmsOrigin, credential)

    init {
        rpcLogin()
    }

    private fun rpcLogin() {
        try {
            rpcClient.login(credential.name, credential.password)
            logger.info("Logged in as ${rpcClient.userName}")
        } catch (e: ClientException) {
            logger.error("Login failed for user ${credential.name} - ${e.message}")
            throw e
        }
    }

    private fun <ReturnType> loginOnRpcError(func: () -> ReturnType): ReturnType {
        try {
            return func()
        } catch (e: ClientException) {
            logger.error("Error from RPC client: ${e.message} - attempting to refresh login session")
            rpcLogin()
            return func()
        }
    }

    fun getContent(contentKeys: IntArray): Document {
        val params = GetContentParams()
        params.contentKeys = contentKeys
        params.includeData = true
        params.includeVersionsInfo = true
        params.includeOfflineContent = true
        params.includeUserRights = false

        return loginOnRpcError { rpcClient.getContent(params) }
    }

    fun getContent(contentKey: Int): Document {
        return getContent(intArrayOf(contentKey))
    }

    fun getContentVersions(versionKeys: IntArray): Document {
        val params = GetContentVersionsParams()
        params.contentVersionKeys = versionKeys
        params.contentRequiredToBeOnline = false

        return loginOnRpcError { rpcClient.getContentVersions(params) }
    }

    fun getContentVersion(versionKey: Int): Document {
        return getContentVersions(intArrayOf(versionKey))
    }

    fun getMenu(menuKey: Int): Document {
        val params = GetMenuParams()
        params.menuKey = menuKey
        params.includeHidden = true

        return loginOnRpcError { rpcClient.getMenu(params) }
    }

    fun getMenuItem(menuItemKey: Int): Document {
        val params = GetMenuItemParams()
        params.menuItemKey = menuItemKey
        params.details = true

        return loginOnRpcError { rpcClient.getMenuItem(params) }
    }

    fun getCategory(categoryKey: Int, depth: Int? = null): Element? {
        val params = GetCategoriesParams()
        params.categoryKey = categoryKey
        params.includeTopCategory = true
        params.levels = depth ?: 1

        return loginOnRpcError { rpcClient.getCategories(params) }
            ?.rootElement
            ?.getChild("category")
    }

    fun getContentByCategory(categoryKey: Int, depth: Int? = null, index: Int? = null, count: Int? = null): Document {
        val params = GetContentByCategoryParams()
        params.categoryKeys = intArrayOf(categoryKey)
        params.includeOfflineContent = true
        params.includeData = false
        params.childrenLevel = depth ?: 1
        params.index = index ?: 0
        params.count = count ?: 100

        return loginOnRpcError { rpcClient.getContentByCategory(params) }
    }

    fun getContentByQuery(query: String): Document {
        val params = GetContentByQueryParams()
        params.includeOfflineContent = true
        params.includeData = false
        params.includeOfflineContent = true
        params.includeVersionsInfo = false
        params.includeUserRights = false
        params.query = query

        return loginOnRpcError { rpcClient.getContentByQuery(params) }
    }

    fun getMenuData(menuKey: Int): Document {
        val params = GetMenuDataParams()
        params.menuKey = menuKey

        return loginOnRpcError { rpcClient.getMenuData(params) }
    }

    suspend fun getPageTemplateKey(contentKey: String, versionKey: String, pageKey: String, unitKey: String): String? {
        return restClient.getPageTemplateKey(contentKey, versionKey, pageKey, unitKey)
    }

    suspend fun renderDocument(document: Document): String? {
        val contentElement = getContentElement(document) ?: return null
        val params = ContentRenderParamsBuilder(contentElement, this).build() ?: return null

        return restClient.renderContent(params)
    }

    suspend fun renderVersion(versionKey: Int): String? {
        return renderDocument(getContentVersion(versionKey))
    }

    suspend fun renderContent(contentKey: Int): String? {
        return renderDocument(getContent(contentKey))
    }
}
