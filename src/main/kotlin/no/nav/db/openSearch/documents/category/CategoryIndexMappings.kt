package no.nav.db.openSearch.documents.category

import no.nav.db.openSearch.documents.IndexMappings
import no.nav.db.openSearch.documents._partials.categoryRef.categoryRefIndexMappings


val categoryIndexMappings: IndexMappings = {
    text(OpenSearchCategoryDocument::xmlAsString)

    keyword(OpenSearchCategoryDocument::key)
    text(OpenSearchCategoryDocument::title)

    keyword(OpenSearchCategoryDocument::contentTypeKey)
    keyword(OpenSearchCategoryDocument::superKey)
    objField(OpenSearchCategoryDocument::categories, null, categoryRefIndexMappings)
}
