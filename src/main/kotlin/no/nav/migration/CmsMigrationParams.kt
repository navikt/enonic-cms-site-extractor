package no.nav.migration

import kotlinx.serialization.Serializable


@Serializable
sealed interface ICmsMigrationParams {
    val key: Int
}

@Serializable
data class CmsCategoryMigrationParams(
    override val key: Int,
    val withChildren: Boolean?,
    val withContent: Boolean?,
    val withVersions: Boolean?
) : ICmsMigrationParams

@Serializable
data class CmsContentMigrationParams(
    override val key: Int,
    val withVersions: Boolean?
) : ICmsMigrationParams

@Serializable
data class CmsVersionMigrationParams(
    override val key: Int,
) : ICmsMigrationParams
