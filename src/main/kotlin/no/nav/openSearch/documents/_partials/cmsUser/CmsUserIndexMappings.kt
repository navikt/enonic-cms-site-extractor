package no.nav.openSearch.documents._partials.cmsUser

import no.nav.openSearch.documents.IndexMappings


val cmsUserIndexMappings: IndexMappings = {
    keyword(CmsUserData::userstore)
    text(CmsUserData::name)
    text(CmsUserData::displayName)
    text(CmsUserData::email)
}