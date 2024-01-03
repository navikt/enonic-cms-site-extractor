package no.nav.db.openSearch.documents.content

import com.jillesvangurp.searchdsls.mappingdsl.FieldMappings


typealias Mappings = (FieldMappings.() -> Unit)

private val cmsUser: Mappings = {
    keyword(CmsUser::userstore)
    text(CmsUser::name)
    text(CmsUser::displayName)
    text(CmsUser::email)
}

private val versions: Mappings = {
    keyword(ContentVersionReference::key)
    keyword(ContentVersionReference::statusKey)
    keyword(ContentVersionReference::status)
    date(ContentVersionReference::timestamp)

    text(ContentVersionReference::title)
    text(ContentVersionReference::comment)

    objField(ContentVersionReference::modifier, null, cmsUser)
}

private val category: Mappings = {
    keyword(ContentCategory::key)
    keyword(ContentCategory::name)
}

private val meta: Mappings = {
    keyword(ContentMetaData::unitKey)
    keyword(ContentMetaData::state)
    keyword(ContentMetaData::status)
    keyword(ContentMetaData::published)
    keyword(ContentMetaData::languageCode)
    keyword(ContentMetaData::languageKey)
    keyword(ContentMetaData::priority)

    keyword(ContentMetaData::contentType)
    keyword(ContentMetaData::contentTypeKey)

    date(ContentMetaData::created)
    date(ContentMetaData::timestamp)
    date(ContentMetaData::publishFrom)
    date(ContentMetaData::publishTo)

    objField(ContentMetaData::category, null, category)

    objField(ContentMetaData::owner, null, cmsUser)
    objField(ContentMetaData::modifier, null, cmsUser)
}

private val locations: Mappings = {
    keyword(ContentLocation::siteKey)
    keyword(ContentLocation::type)
    keyword(ContentLocation::menuItemKey)
    keyword(ContentLocation::menuItemName)
    keyword(ContentLocation::menuItemPath)
    text(ContentLocation::menuItemDisplayName)
    bool(ContentLocation::home)
}

val contentIndexMappings: Mappings = {
    keyword(OpenSearchContentDocument::path)

    text(OpenSearchContentDocument::html)
    text(OpenSearchContentDocument::xmlAsString)

    keyword(OpenSearchContentDocument::contentKey)
    keyword(OpenSearchContentDocument::versionKey)
    bool(OpenSearchContentDocument::isCurrentVersion)

    text(OpenSearchContentDocument::name)
    text(OpenSearchContentDocument::displayName)

    objField(OpenSearchContentDocument::versions, null, versions)
    objField(OpenSearchContentDocument::locations, null, locations)
    objField(OpenSearchContentDocument::meta, null, meta)
}
