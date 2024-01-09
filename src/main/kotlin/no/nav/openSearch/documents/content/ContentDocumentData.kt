package no.nav.openSearch.documents.content

import CategoryRefData
import no.nav.openSearch.documents._partials.cmsUser.CmsUserData
import kotlinx.serialization.Serializable


@Serializable
data class ContentVersionReference(
    val key: String,
    val statusKey: String?,
    val status: String?,
    val timestamp: String?,
    val title: String?,
    val comment: String?,
    val modifier: CmsUserData?,
)

@Serializable
data class ContentBinaryReference(
    val key: String,
    val filename: String,
    val filesize: Int
)

@Serializable
data class ContentLocation(
    val siteKey: String?,
    val type: String?,
    val menuItemKey: String?,
    val menuItemName: String?,
    val menuItemPath: String?,
    val menuItemDisplayName: String?,
    val home: Boolean?,
)

@Serializable
data class ContentMetaData(
    val unitKey: String?,
    val state: String?,
    val status: String?,
    val published: String?,
    val languageCode: String?,
    val languageKey: String?,
    val priority: String?,

    val contentType: String?,
    val contentTypeKey: String?,

    val created: String?,
    val timestamp: String?,
    val publishFrom: String?,
    val publishTo: String?,

    val owner: CmsUserData?,
    val modifier: CmsUserData?,
)

@Serializable
data class OpenSearchContentDocument(
    val html: String?,
    val xmlAsString: String,

    val contentKey: String,
    val versionKey: String,
    val isCurrentVersion: Boolean,

    val name: String,
    val displayName: String,

    val versions: List<ContentVersionReference>?,
    val locations: List<ContentLocation>?,
    val category: CategoryRefData?,
    val binaries: List<ContentBinaryReference>?,

    val meta: ContentMetaData,
)