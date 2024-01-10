package no.nav.migration

import kotlinx.serialization.Serializable
import no.nav.cms.client.CmsClient
import org.jdom.Element


@Serializable
data class CmsDocumentsCount(
    val categoriesList: MutableList<String>,
    val contentsList: MutableList<String>,
    val versionsList: MutableList<String>,
    val binariesList: MutableList<String>
) {
    val numCategories: Int = categoriesList.size
    val numContents: Int = contentsList.size
    val numVersions: Int = versionsList.size
    val numBinaries: Int = binariesList.size
}

class CmsMigrationDocumentCounter(private val params: ICmsMigrationParams, private val cmsClient: CmsClient) {
    private val categoriesList: MutableList<String> = mutableListOf()
    private val contentsList: MutableList<String> = mutableListOf()
    private val versionsList: MutableList<String> = mutableListOf()
    private val binariesList: MutableList<String> = mutableListOf()

    fun runCount(): CmsMigrationDocumentCounter {
        when (params) {
            is CmsCategoryMigrationParams -> countCategories(params)
            is CmsContentMigrationParams -> countContents(params)
            is CmsVersionMigrationParams -> countVersions(params)
        }

        return this
    }

    fun getCount(): CmsDocumentsCount {
        return CmsDocumentsCount(
            categoriesList,
            contentsList,
            versionsList,
            binariesList
        )
    }

    private fun countCategories(params: CmsCategoryMigrationParams) {
        val categoryElement = cmsClient.getCategory(params.key, 1) ?: return

        categoriesList.add(params.key.toString())

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

    private fun countContents(contentElement: Element?, withVersions: Boolean?) {
        val contentKey = contentElement?.getAttributeValue("key") ?: return

        contentsList.add(contentKey)

        countBinaries(contentElement)

        if (withVersions == true) {
            countVersions(contentElement)
        }
    }

    private fun countContents(params: CmsContentMigrationParams) {
        countContents(
            cmsClient.getContent(params.key)?.rootElement,
            params.withVersions
        )
    }

    private fun countVersions(contentElement: Element?) {
        val versions = contentElement
            ?.getChild("versions")
            ?.getChildren("version")
            ?.filterIsInstance<Element>()
            ?.mapNotNull { versionElement ->
                versionElement.getAttributeValue("key")
            }

        if (versions !== null) {
            versionsList.addAll(versions)
        }
    }

    private fun countVersions(params: CmsVersionMigrationParams) {
        if (cmsClient.getContentVersion(params.key) !== null) {
            versionsList.add(params.key.toString())
        }
    }

    private fun countBinaries(contentElement: Element) {
        val binaryKeys = contentElement
            .getChild("binaries")
            ?.getChildren("binary")
            ?.filterIsInstance<Element>()
            ?.mapNotNull {
                it.getAttributeValue("key")
            }

        if (binaryKeys != null) {
            binariesList.addAll(binaryKeys)
        }
    }
}