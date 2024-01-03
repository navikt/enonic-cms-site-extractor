package no.nav.db.openSearch.documents.category

import kotlinx.serialization.Serializable


@Serializable
data class OpenSearchCategoryDocument(
    val xmlAsString: String,

    val key: String,
    val title: String,

    val contentTypeKey: String?,
    val superKey: String?,
    val childrenKeys: List<String>?
)
