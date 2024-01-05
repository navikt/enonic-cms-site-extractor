package no.nav.migration


sealed interface CmsMigratorParams {
    val key: Int
}

data class CmsCategoryMigratorParams(
    override val key: Int,
    val withChildren: Boolean?,
    val withContent: Boolean?,
    val withVersions: Boolean?
) : CmsMigratorParams

data class CmsContentMigratorParams(
    override val key: Int,
    val withVersions: Boolean?
) : CmsMigratorParams

data class CmsVersionMigratorParams(
    override val key: Int,
) : CmsMigratorParams
