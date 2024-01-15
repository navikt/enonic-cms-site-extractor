package no.nav.migration

import CmsDocumentsKeys
import no.nav.cms.client.CmsClient
import no.nav.cms.utils.getCategoryElement
import no.nav.cms.utils.getChildElements
import no.nav.cms.utils.getContentElement
import org.jdom.Element


class CmsMigrationDocumentsEnumerator(private val params: ICmsMigrationParams, private val cmsClient: CmsClient) {
    private val categories: MutableSet<Int> = mutableSetOf()
    private val contents: MutableSet<Int> = mutableSetOf()
    private val versions: MutableSet<Int> = mutableSetOf()
    private val binaries: MutableSet<Int> = mutableSetOf()

    fun run(): CmsDocumentsKeys {
        when (params) {
            is CmsCategoryMigrationParams -> countCategories(params)
            is CmsContentMigrationParams -> countContents(params)
            is CmsVersionMigrationParams -> countVersions(params)
        }

        return CmsDocumentsKeys(categories, contents, versions, binaries)
    }

    private fun countCategories(params: CmsCategoryMigrationParams) {
        val categoryElement = cmsClient.getCategory(params.key, 1)
            ?.run { getCategoryElement(this) }
            ?: return

        categories.add(params.key)

        if (params.withContent == true) {
            cmsClient.getContentByCategory(params.key, 1, includeVersionsInfo = true)
                ?.rootElement
                ?.run { getChildElements(this, "content") }
                ?.forEach { contentElement ->
                    countContents(contentElement, params.withVersions)
                }
        }

        if (params.withChildren == true) {
            categoryElement
                .getChild("categories")
                ?.run { getChildElements(this, "category") }
                ?.forEach {
                    val key = it.getAttributeValue("key")?.toInt() ?: return

                    countCategories(
                        CmsCategoryMigrationParams(
                            key,
                            withChildren = params.withChildren,
                            withContent = params.withContent,
                            withVersions = params.withVersions,
                        )
                    )
                }
        }
    }

    private fun countContents(contentElement: Element, withVersions: Boolean?) {
        val contentKey = contentElement
            .getAttributeValue("key")
            .toInt()

        contents.add(contentKey)

        countBinaries(contentElement)

        if (withVersions == true) {
            countVersions(contentElement)
        }
    }

    private fun countContents(params: CmsContentMigrationParams) {
        val document = cmsClient.getContent(params.key) ?: return

        val contentElement = getContentElement(document) ?: return

        countContents(
            contentElement,
            params.withVersions
        )
    }

    private fun countVersions(contentElement: Element) {
        val contentVersionKey = contentElement.getAttributeValue("versionkey")

        val versions = contentElement
            .getChild("versions")
            ?.run { getChildElements(this, "version") }
            ?.mapNotNull { versionElement ->
                val versionKey = versionElement.getAttributeValue("key")

                if (versionKey == contentVersionKey) {
                    return@mapNotNull null
                }

                versionKey.toInt()
            }

        if (versions !== null) {
            this.versions.addAll(versions)
        }
    }

    private fun countVersions(params: CmsVersionMigrationParams) {
        if (cmsClient.getContentVersion(params.key) !== null) {
            versions.add(params.key)
        }
    }

    // This only includes binaries from the most recent content versions
    private fun countBinaries(contentElement: Element) {
        val binaryKeys = contentElement
            .getChild("binaries")
            ?.run { getChildElements(this, "binary") }
            ?.mapNotNull {
                it.getAttributeValue("key")?.toInt()
            }

        if (binaryKeys != null) {
            binaries.addAll(binaryKeys)
        }
    }
}