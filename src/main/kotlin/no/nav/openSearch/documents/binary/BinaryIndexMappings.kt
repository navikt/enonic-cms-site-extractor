package no.nav.openSearch.documents.binary

import no.nav.openSearch.documents.IndexMappings


val binaryIndexMappings: IndexMappings = {
    keyword(OpenSearchBinaryDocument::binaryKey)
    keyword(OpenSearchBinaryDocument::contentKey)
    keyword(OpenSearchBinaryDocument::versionKey)

    text(OpenSearchBinaryDocument::filename)
    number<Int>(OpenSearchBinaryDocument::filesize)

    field(OpenSearchBinaryDocument::data, "binary")
}
