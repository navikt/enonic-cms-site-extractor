package no.nav.openSearch.documents.binary

import kotlinx.serialization.Serializable


@Serializable
data class OpenSearchBinaryDocument(
    val binaryKey: String,
    val contentKey: String,
    val versionKeys: List<String>,

    val filename: String,
    val filesize: Int,

    val data: String
)