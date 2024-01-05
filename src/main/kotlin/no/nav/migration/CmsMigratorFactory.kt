package no.nav.migration

import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.cms.client.CmsClientBuilder
import no.nav.db.openSearch.OpenSearchClientBuilder


private val logger = KtorSimpleLogger("CmsMigratorFactory")

object CmsMigratorFactory {
    private val categoryIndexers = HashMap<Int, CmsMigrator>()
    private val contentIndexers = HashMap<Int, CmsMigrator>()
    private val versionIndexers = HashMap<Int, CmsMigrator>()

    suspend fun createOrRetrieveMigrator(
        params: CmsMigratorParams,
        environment: ApplicationEnvironment?,
    ): CmsMigrator? {
        val key = params.key
        val migratorMap = when (params) {
            is CmsCategoryMigratorParams -> categoryIndexers
            is CmsContentMigratorParams -> contentIndexers
            is CmsVersionMigratorParams -> versionIndexers
        }

        val existingMigrator = migratorMap[key]
        if (existingMigrator != null) {
            return existingMigrator
        }

        val cmsClient = CmsClientBuilder(environment).build()
        val openSearchClient = OpenSearchClientBuilder(environment).build()

        if (cmsClient == null || openSearchClient == null) {
            logger.error("Failed to initialize required clients")
            return null
        }

        val migrator = CmsMigrator(cmsClient, openSearchClient, params)

        migratorMap[key] = migrator

        return migrator
    }
}
