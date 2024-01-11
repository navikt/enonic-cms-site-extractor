package no.nav.migration

import kotlinx.serialization.Serializable
import no.nav.cms.client.CmsClient
import no.nav.cms.utils.getContentElement
import org.jdom.Element


@Serializable
data class CmsDocumentsKeys(
    val categories: MutableSet<Int>,
    val contents: MutableSet<Int>,
    val versions: MutableSet<Int>,
    val binaries: MutableSet<Int>
)

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
        val categoryElement = cmsClient.getCategory(params.key, 1) ?: return

        categories.add(params.key)

        cmsClient.getContentByCategory(params.key, 1, includeVersionsInfo = true)
            ?.rootElement
            ?.getChildren("content")
            ?.filterIsInstance<Element>()
            ?.forEach { contentElement ->
                countContents(contentElement, params.withVersions)
            }

        categoryElement
            .getChild("categories")
            ?.getChildren("category")
            ?.filterIsInstance<Element>()
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
            ?.getChildren("version")
            ?.filterIsInstance<Element>()
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

    private fun countBinaries(contentElement: Element) {
        val binaryKeys = contentElement
            .getChild("binaries")
            ?.getChildren("binary")
            ?.filterIsInstance<Element>()
            ?.mapNotNull {
                it.getAttributeValue("key")?.toInt()
            }

        if (binaryKeys != null) {
            binaries.addAll(binaryKeys)
        }
    }
}