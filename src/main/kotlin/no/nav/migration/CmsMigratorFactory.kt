package no.nav.migration

import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.cms.client.CmsClientBuilder
import no.nav.openSearch.OpenSearchClientBuilder
import kotlin.collections.HashMap


private val logger = KtorSimpleLogger("CmsMigratorFactory")

enum class CmsMigratorType {
    CATEGORY, CONTENT, VERSION
}

object CmsMigratorFactory {
    private val categoryMigrators = HashMap<Int, CmsMigrator>()
    private val contentMigrators = HashMap<Int, CmsMigrator>()
    private val versionMigrators = HashMap<Int, CmsMigrator>()

    suspend fun createOrRetrieveMigrator(
        params: ICmsMigrationParams,
        environment: ApplicationEnvironment?,
        forceCreate: Boolean? = false
    ): CmsMigrator? {
        val key = params.key
        val migratorMap = when (params) {
            is CmsCategoryMigrationParams -> categoryMigrators
            is CmsContentMigrationParams -> contentMigrators
            is CmsVersionMigrationParams -> versionMigrators
        }

        if (forceCreate != true) {
            val existingMigrator = migratorMap[key]
            if (existingMigrator != null) {
                return existingMigrator
            }
        }

        val cmsClient = CmsClientBuilder(environment).build()
        if (cmsClient == null) {
            logger.error("Failed to initialize CMS client")
            return null
        }

        val openSearchClient = OpenSearchClientBuilder(environment).build()
        if (openSearchClient == null) {
            logger.error("Failed to initialize OpenSearch client")
            return null
        }

        val migrator = CmsMigrator(params, cmsClient, openSearchClient)

        migratorMap[key] = migrator

        return migrator
    }

    private fun getMigrator(key: Int, type: CmsMigratorType): CmsMigrator? {
        return when (type) {
            CmsMigratorType.CATEGORY -> categoryMigrators
            CmsMigratorType.CONTENT -> contentMigrators
            CmsMigratorType.VERSION -> versionMigrators
        }[key]
    }

    suspend fun abortJob(key: Int, type: CmsMigratorType): Boolean {
        val migrator = getMigrator(key, type)
        if (migrator == null) {
            logger.info("No migration job found for $key of type ${type.name}")
            return false
        }

        migrator.abort()

        return true
    }

    fun getStatus(
        key: Int,
        type: CmsMigratorType,
        withResults: Boolean?,
        withRemaining: Boolean?
    ): CmsMigrationStatusData? {
        val migrator = getMigrator(key, type)
        if (migrator == null) {
            logger.info("No migration job found for $key of type ${type.name}")
            return null
        }

        return migrator.getStatus(withResults, withRemaining)
    }
}
