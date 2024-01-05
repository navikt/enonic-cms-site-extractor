package no.nav.openSearch.documents.category

import CategoryRefData
import kotlinx.serialization.Serializable


@Serializable
data class ContentRefData(
    val key: String,
    val name: String,
    val displayName: String,
    val timestamp: String?,
)

@Serializable
data class OpenSearchCategoryDocument(
    val xmlAsString: String,

    val key: String,
    val title: String,

    val contentTypeKey: String?,
    val superKey: String?,

    val categories: List<CategoryRefData>?,
    val contents: List<ContentRefData>?
)
