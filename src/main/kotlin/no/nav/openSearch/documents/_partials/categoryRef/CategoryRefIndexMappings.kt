package no.nav.openSearch.documents._partials.categoryRef

import CategoryRefData
import no.nav.openSearch.documents.IndexMappings


val categoryRefIndexMappings: IndexMappings = {
    keyword(CategoryRefData::key)
    keyword(CategoryRefData::name)
}
