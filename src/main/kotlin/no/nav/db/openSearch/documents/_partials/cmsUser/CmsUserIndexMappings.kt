package no.nav.db.openSearch.documents._partials.cmsUser

import no.nav.db.openSearch.documents.IndexMappings


val cmsUserIndexMappings: IndexMappings = {
    keyword(CmsUserData::userstore)
    text(CmsUserData::name)
    text(CmsUserData::displayName)
    text(CmsUserData::email)
}