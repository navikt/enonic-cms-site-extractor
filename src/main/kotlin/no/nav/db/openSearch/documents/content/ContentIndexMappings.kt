package no.nav.db.openSearch.documents.content

import no.nav.db.openSearch.documents.IndexMappings
import no.nav.db.openSearch.documents._partials.categoryRef.categoryRefIndexMappings
import no.nav.db.openSearch.documents._partials.cmsUser.cmsUserIndexMappings


private val versions: IndexMappings = {
    keyword(ContentVersionReference::key)
    keyword(ContentVersionReference::statusKey)
    keyword(ContentVersionReference::status)
    date(ContentVersionReference::timestamp)

    text(ContentVersionReference::title)
    text(ContentVersionReference::comment)

    objField(ContentVersionReference::modifier, null, cmsUserIndexMappings)
}

private val meta: IndexMappings = {
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

    objField(ContentMetaData::category, null, categoryRefIndexMappings)

    objField(ContentMetaData::owner, null, cmsUserIndexMappings)
    objField(ContentMetaData::modifier, null, cmsUserIndexMappings)
}

private val locations: IndexMappings = {
    keyword(ContentLocation::siteKey)
    keyword(ContentLocation::type)
    keyword(ContentLocation::menuItemKey)
    keyword(ContentLocation::menuItemName)
    keyword(ContentLocation::menuItemPath)
    text(ContentLocation::menuItemDisplayName)
    bool(ContentLocation::home)
}

val contentIndexMappings: IndexMappings = {
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
