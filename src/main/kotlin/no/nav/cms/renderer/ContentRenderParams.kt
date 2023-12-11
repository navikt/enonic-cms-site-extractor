package no.nav.cms.renderer

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import org.jdom.Element

private const val CT_KEY_PAGE_KEY_DELTA = 999

private val logger = KtorSimpleLogger("ContentRenderParams")

data class ContentRenderParams(
    val contentkey: String,
    val versionkey: String,
    val page: String,
    val selectedunitkey: String,
    val menukey: String,
    val menuitemkey: String,
    val pagetemplatekey: String
)

class ContentRenderParamsBuilder(contentElement: Element, cmsClient: CmsClient) {
    private val cmsClient: CmsClient
    private val contentElement: Element

    init {
        this.cmsClient = cmsClient
        this.contentElement = contentElement
    }

    suspend fun build(): ContentRenderParams? {
        try {
            val contentKey = getContentKey()
            logger.info("Content key: $contentKey")

            val versionKey = getVersionKey()
            logger.info("Version key: $versionKey")

            val pageKey = getPageKey()
            logger.info("Page key: $pageKey")

            val unitKey = getUnitKey()
            logger.info("Unit key: $unitKey")

            val siteKey = getSiteKey() ?: "20"
            logger.info("Site key: $siteKey")

            val menuItemKey = getMenuItemKey() ?: "16065"
            logger.info("Menuitem key: $menuItemKey")

            val pageTemplateKey = getPageTemplateKey(contentKey, versionKey, pageKey, unitKey) ?: "553"
            logger.info("Pagetemplate key: $pageTemplateKey")

            return ContentRenderParams(
                contentkey = contentKey,
                versionkey = versionKey,
                page = pageKey,
                selectedunitkey = unitKey,
                menukey = siteKey,
                menuitemkey = menuItemKey,
                pagetemplatekey = pageTemplateKey
            )
        } catch (e: Exception) {
            logger.error("Error building ContentRenderParams: ${e.message}")
            return null
        }
    }

    private fun getContentKey(): String {
        return this.contentElement.getAttribute("key").value
    }

    private fun getSiteKey(): String? {
        return this.contentElement
            .getChild("location")
            ?.getChild("site")
            ?.getAttribute("key")
            ?.value
    }

    private fun getMenuItemKey(): String? {
        return this.contentElement
            .getChild("location")
            ?.getChild("site")
            ?.getChild("contentlocation")
            ?.getAttribute("menuitemkey")
            ?.value
    }

    private fun getUnitKey(): String {
        return this.contentElement
            .getAttribute("unitkey")
            .value
    }

    private fun getVersionKey(): String {
        return this.contentElement
            .getAttribute("versionkey")
            .value
    }

    private fun getContentTypeKey(): String {
        return this.contentElement
            .getAttribute("contenttypekey")
            .value
    }

    private fun getPageKey(): String {
        return (this.getContentTypeKey().toInt() + CT_KEY_PAGE_KEY_DELTA).toString()
    }

    private suspend fun getPageTemplateKey(
        contentKey: String,
        versionKey: String,
        pageKey: String,
        unitKey: String
    ): String? {
        val result = this.cmsClient
            .getPageTemplateKey(contentKey, versionKey, pageKey, unitKey)

        if (result == null) {
            logger.info("Could not retrieve pageTemplateKey for $contentKey $versionKey $pageKey $unitKey")
        }

        return result
    }
}