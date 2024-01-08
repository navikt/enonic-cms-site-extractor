package no.nav.openSearch.documents.binary

import no.nav.openSearch.documents.IndexMappings


val binaryIndexMappings: IndexMappings = {
    keyword(OpenSearchBinaryDocument::binaryKey)
    keyword(OpenSearchBinaryDocument::contentKey)
    keyword(OpenSearchBinaryDocument::versionKey)

    bool(OpenSearchBinaryDocument::isCurrentVersion)

    text(OpenSearchBinaryDocument::filename)
    number<Int>(OpenSearchBinaryDocument::filesize)
    date(OpenSearchBinaryDocument::timestamp)

    field(OpenSearchBinaryDocument::data, "binary")
}
