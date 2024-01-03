package no.nav.db.openSearch.documents._partials.categoryRef

import CategoryRefData
import no.nav.db.openSearch.documents.IndexMappings


val categoryRefIndexMappings: IndexMappings = {
    keyword(CategoryRefData::key)
    keyword(CategoryRefData::name)
}
