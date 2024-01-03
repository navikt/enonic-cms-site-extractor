package no.nav.db.openSearch.documents.category

import com.jillesvangurp.searchdsls.mappingdsl.FieldMappings


typealias Mappings = (FieldMappings.() -> Unit)

val categoryIndexMappings: Mappings = {
    text(OpenSearchCategoryDocument::xmlAsString)

    keyword(OpenSearchCategoryDocument::key)
    keyword(OpenSearchCategoryDocument::contentTypeKey)
    keyword(OpenSearchCategoryDocument::superKey)

    text(OpenSearchCategoryDocument::title)

//    objField(OpenSearchContentDocument::versions, null, versions)
}
