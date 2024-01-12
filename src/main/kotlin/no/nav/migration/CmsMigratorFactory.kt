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

typealias MigratorsMap = HashMap<Int, CmsMigrator>

object CmsMigratorFactory {
    private val categoryMigrators = MigratorsMap()
    private val contentMigrators = MigratorsMap()
    private val versionMigrators = MigratorsMap()

    private val waitingForInit = mutableSetOf<Int>()

    suspend fun createOrRetrieveMigrator(
        params: ICmsMigrationParams,
        environment: ApplicationEnvironment?,
        forceCreate: Boolean? = false,
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

        if (waitingForInit.contains(key)) {
            logger.warn("Migrator for key $key is currently initializing")
            return null
        }

        waitingForInit.add(key)

        val migrator = try {
            CmsMigrator(params, cmsClient, openSearchClient)
        } catch (e: Exception) {
            logger.error("Error while initalizing CMS migrator: ${e.message}")
            throw e
        } finally {
            waitingForInit.remove(key)
        }

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

    private fun removeInactive(migratorsMap: MigratorsMap): Int {
        return migratorsMap.keys.fold(0) { acc, key ->
            val migrator = migratorsMap[key]

            if (migrator?.state != CmsMigratorState.RUNNING) {
                migratorsMap.remove(key)
                return@fold acc + 1
            }

            acc
        }
    }

    fun cleanup(): Int {
        return removeInactive(categoryMigrators) +
                removeInactive(contentMigrators) +
                removeInactive(versionMigrators)
    }
}
