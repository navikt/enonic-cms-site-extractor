package no.nav.db.openSearch.documents.category

import no.nav.db.openSearch.documents.IndexMappings
import no.nav.db.openSearch.documents._partials.categoryRef.categoryRefIndexMappings

private val contentRefIndexMappings: IndexMappings = {
    keyword(ContentRefData::key)
    keyword(ContentRefData::name)
    text(ContentRefData::displayName)
    date(ContentRefData::timestamp)
}

val categoryIndexMappings: IndexMappings = {
    text(OpenSearchCategoryDocument::xmlAsString)

    keyword(OpenSearchCategoryDocument::key)
    text(OpenSearchCategoryDocument::title)

    keyword(OpenSearchCategoryDocument::contentTypeKey)
    keyword(OpenSearchCategoryDocument::superKey)

    objField(OpenSearchCategoryDocument::categories, null, categoryRefIndexMappings)
    objField(OpenSearchCategoryDocument::contents, null, contentRefIndexMappings)
}
