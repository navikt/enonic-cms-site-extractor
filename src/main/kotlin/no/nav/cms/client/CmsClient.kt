package no.nav.cms.client

import com.enonic.cms.api.client.ClientException
import com.enonic.cms.api.client.ClientFactory
import com.enonic.cms.api.client.RemoteClient
import com.enonic.cms.api.client.model.GetBinaryParams
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


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

    private fun <ReturnType> rpcErrorHandler(func: () -> ReturnType): ReturnType? {
        try {
            return func()
        } catch (e: ClientException) {
            logger.error("Error from RPC client: ${e.message}")
            return null
        }
    }

    fun getContent(contentKeys: IntArray): Document? {
        val params = GetContentParams()
        params.contentKeys = contentKeys
        params.includeData = true
        params.includeVersionsInfo = true
        params.includeOfflineContent = true
        params.includeUserRights = false

        return rpcErrorHandler { rpcClient.getContent(params) }
    }

    fun getContent(contentKey: Int): Document? {
        return getContent(intArrayOf(contentKey))
    }

    fun getContentVersions(versionKeys: IntArray): Document? {
        val params = GetContentVersionsParams()
        params.contentVersionKeys = versionKeys
        params.contentRequiredToBeOnline = false

        return rpcErrorHandler { rpcClient.getContentVersions(params) }
    }

    fun getContentVersion(versionKey: Int): Document? {
        return getContentVersions(intArrayOf(versionKey))
    }

    fun getMenu(menuKey: Int): Document? {
        val params = GetMenuParams()
        params.menuKey = menuKey
        params.includeHidden = true

        return rpcErrorHandler { rpcClient.getMenu(params) }
    }

    fun getMenuItem(menuItemKey: Int): Document? {
        val params = GetMenuItemParams()
        params.menuItemKey = menuItemKey
        params.details = true

        return rpcErrorHandler { rpcClient.getMenuItem(params) }
    }

    fun getCategory(categoryKey: Int, depth: Int? = null): Element? {
        val params = GetCategoriesParams()
        params.categoryKey = categoryKey
        params.includeTopCategory = true
        params.levels = depth ?: 1

        return rpcErrorHandler { rpcClient.getCategories(params) }
            ?.rootElement
            ?.getChild("category")
    }

    fun getContentByCategory(
        categoryKey: Int,
        depth: Int? = null,
        index: Int? = null,
        count: Int? = null,
        includeVersionsInfo: Boolean = false
    ): Document? {
        val params = GetContentByCategoryParams()
        params.categoryKeys = intArrayOf(categoryKey)
        params.includeOfflineContent = true
        params.includeData = false
        params.levels = depth ?: 1
        params.index = index ?: 0
        params.count = count ?: 1000
        params.includeVersionsInfo = includeVersionsInfo

        return rpcErrorHandler { rpcClient.getContentByCategory(params) }
    }

    fun getContentByQuery(query: String): Document? {
        val params = GetContentByQueryParams()
        params.includeData = false
        params.includeOfflineContent = true
        params.includeVersionsInfo = false
        params.includeUserRights = false
        params.query = query

        return rpcErrorHandler { rpcClient.getContentByQuery(params) }
    }

    fun getMenuData(menuKey: Int): Document? {
        val params = GetMenuDataParams()
        params.menuKey = menuKey

        return rpcErrorHandler { rpcClient.getMenuData(params) }
    }

    fun getBinary(binaryKey: Int): Document? {
        val params = GetBinaryParams()
        params.binaryKey = binaryKey

        return rpcErrorHandler { rpcClient.getBinary(params) }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getBinaryDataAsBase64(binaryKey: Int, contentKey: Int, versionKey: Int): String? {
        val rawData = restClient.getAttachmentData(binaryKey, contentKey, versionKey) ?: return null

        return Base64.encode(rawData)
    }

    suspend fun getDefaultContentLocationKeys(
        contentKey: String,
        versionKey: String,
        pageKey: String,
        unitKey: String
    ): ContentLocationKeys? {
        return restClient.getDefaultContentLocationKeys(contentKey, versionKey, pageKey, unitKey)
    }

    suspend fun renderDocument(document: Document?): String? {
        if (document == null) {
            return null
        }

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
