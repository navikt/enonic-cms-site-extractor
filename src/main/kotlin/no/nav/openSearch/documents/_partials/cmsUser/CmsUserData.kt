package no.nav.openSearch.documents._partials.cmsUser

import kotlinx.serialization.Serializable


@Serializable
data class CmsUserData(
    val userstore: String?,
    val name: String?,
    val displayName: String?,
    val email: String?,
)
