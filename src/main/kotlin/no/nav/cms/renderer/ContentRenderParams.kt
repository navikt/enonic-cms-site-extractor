package no.nav.cms.renderer

import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.utils.xmlToString
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

class ContentRenderParamsBuilder(
    private val contentElement: Element,
    private val cmsClient: CmsClient
) {

    suspend fun build(): ContentRenderParams? {
        try {
            val contentKey = getContentKey()
            if (contentKey == null) {
                logger.error("No contentKey found for provided element ${xmlToString(contentElement)}")
                return null
            }

            val versionKey = getVersionKey()
            if (versionKey == null) {
                logger.error("No versionKey found for content $contentKey")
                return null
            }

            val pageKey = getPageKey()
            if (pageKey == null) {
                logger.error("No pageKey found for content $contentKey (version: $versionKey)")
                return null
            }

            val unitKey = getUnitKey()
            if (unitKey == null) {
                logger.error("No unitKey found for content $contentKey (version: $versionKey)")
                return null
            }

            val siteKey = getSiteKey()
            if (siteKey == null) {
                logger.error("No siteKey found for content $contentKey (version: $versionKey)")
                return null
            }

            val menuItemKey = getMenuItemKey()
            if (menuItemKey == null) {
                logger.error("No menuItemKey found for content $contentKey (version: $versionKey)")
                return null
            }

            val pageTemplateKey = getPageTemplateKey(contentKey, versionKey, pageKey, unitKey)
            if (pageTemplateKey == null) {
                logger.error("No pageTemplateKey found for content $contentKey (version: $versionKey)")
                return null
            }

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

    private fun getContentKey(): String? {
        return this.contentElement.getAttribute("key")?.value
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

    private fun getUnitKey(): String? {
        return this.contentElement
            .getAttribute("unitkey")
            ?.value
    }

    private fun getVersionKey(): String? {
        return this.contentElement
            .getAttribute("versionkey")
            ?.value
    }

    private fun getContentTypeKey(): String? {
        return this.contentElement
            .getAttribute("contenttypekey")
            ?.value
    }

    private fun getPageKey(): String? {
        val contentTypeKey = this.getContentTypeKey() ?: return null

        return (contentTypeKey.toInt() + CT_KEY_PAGE_KEY_DELTA).toString()
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