package no.nav.db.openSearch.index

import java.util.*


enum class ContentDocumentType {
    CURRENT, VERSION
}

data class CmsUser(
    val userstore: String,
    val name: String,
    val displayName: String,
    val email: String,
)

data class ContentCategory(
    val key: Int,
    val name: String,
)

data class VersionReference(
    val key: Int,
    val statusKey: Int,
    val status: String,
    val timestamp: Date,

    val title: String,
    val comment: String,
    val modifier: CmsUser,
)

data class ContentLocation(
    val siteKey: Int,

    val type: String,
    val menuItemKey: Int,
    val menuItemName: String,
    val menuItemPath: String,
    val menuItemDisplayName: String,
    val home: Boolean
)

data class ContentMetaData(
    val unitKey: Int,
    val state: Int,
    val status: Int,
    val published: String,
    val current: Boolean,
    val languageCode: String,
    val languageKey: Int,
    val priority: Int,

    val contentType: String,
    val contentTypeKey: Int,

    val created: Date,
    val timestamp: Date,
    val publishFrom: Date,
    val publishTo: Date,

    val category: ContentCategory,
)

data class ContentDocument(
    val type: ContentDocumentType,
    val paths: List<String>,

    val html: String?,
    val xmlAsString: String,

    val contentKey: Int,
    val versionKey: Int,

    val name: String,
    val displayName: String,

    val owner: CmsUser,
    val modifier: CmsUser,

    val versions: List<VersionReference>,
    val locations: List<ContentLocation>,

    val meta: ContentMetaData,
)