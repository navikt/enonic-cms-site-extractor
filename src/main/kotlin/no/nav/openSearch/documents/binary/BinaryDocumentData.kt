package no.nav.openSearch.documents.binary

import kotlinx.serialization.Serializable


@Serializable
data class OpenSearchBinaryDocument(
    val binaryKey: String,
    val contentKey: String,
    val versionKey: String,

    val isCurrentVersion: Boolean,

    val filename: String,
    val filesize: Int,
    val timestamp: String,

    val data: String
)