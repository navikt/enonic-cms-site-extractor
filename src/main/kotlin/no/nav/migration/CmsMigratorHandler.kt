package no.nav.migration

import CmsMigrationStatusSummary
import io.ktor.server.application.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import no.nav.cms.client.CmsClientBuilder
import no.nav.migration.status.CmsMigrationStatusBuilder
import no.nav.openSearch.OpenSearchClientBuilder
import kotlin.collections.HashMap


private typealias MigratorsMap = HashMap<Int, CmsMigrator>

private val logger = KtorSimpleLogger("CmsMigratorFactory")

enum class CmsMigratorType {
    CATEGORY, CONTENT, VERSION
}

@Serializable
data class CmsMigratorStatusAll(
    val categories: List<CmsMigrationStatusSummary>,
    val contents: List<CmsMigrationStatusSummary>,
    val versions: List<CmsMigrationStatusSummary>,
)

object CmsMigratorHandler {
    private val categoryMigrators = MigratorsMap()
    private val contentMigrators = MigratorsMap()
    private val versionMigrators = MigratorsMap()

    private val waitingForInit = mutableSetOf<Int>()

    private fun getMigrators(params: ICmsMigrationParams): MigratorsMap {
        return when (params) {
            is CmsCategoryMigrationParams -> categoryMigrators
            is CmsContentMigrationParams -> contentMigrators
            is CmsVersionMigrationParams -> versionMigrators
        }
    }

    private fun getMigrator(key: Int, type: CmsMigratorType): CmsMigrator? {
        return when (type) {
            CmsMigratorType.CATEGORY -> categoryMigrators
            CmsMigratorType.CONTENT -> contentMigrators
            CmsMigratorType.VERSION -> versionMigrators
        }[key]
    }

    fun getMigratorState(params: ICmsMigrationParams): String? {
        if (waitingForInit.contains(params.key)) {
            return "initializing"
        }

        return getMigrators(params)[params.key]?.state?.name
    }

    suspend fun initMigrator(
        params: ICmsMigrationParams,
        environment: ApplicationEnvironment?,
        start: Boolean? = false,
        forceCreate: Boolean? = false,
    ) {
        createOrRetrieveMigrator(params, environment, forceCreate)?.apply {
            if (start == true) {
                this.run()
            }
        }
    }

    private suspend fun createOrRetrieveMigrator(
        params: ICmsMigrationParams,
        environment: ApplicationEnvironment?,
        forceCreate: Boolean? = false,
    ): CmsMigrator? {
        val key = params.key

        val migrators = getMigrators(params)

        migrators[key]?.run {
            if (forceCreate != true) {
                return@createOrRetrieveMigrator this
            }

            this.abort()
            migrators.remove(key)
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
            logger.info("Migrator for key $key is currently initializing")
            return null
        }

        waitingForInit.add(key)

        val migrator = try {
            val status = CmsMigrationStatusBuilder(cmsClient, openSearchClient).build(params)
            CmsMigrator(status, cmsClient, openSearchClient)
        } catch (e: Exception) {
            logger.error("Error while initalizing CMS migrator: ${e.message}")
            throw e
        } finally {
            waitingForInit.remove(key)
        }

        migrators[key] = migrator

        return migrator
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

    suspend fun abortAll() {
        listOf(categoryMigrators, contentMigrators, versionMigrators).forEach { migratorsMap ->
            migratorsMap.values.forEach { migrator ->
                if (migrator.state == CmsMigratorState.RUNNING) {
                    migrator.abort()
                }
            }
        }
    }

    fun getStatus(key: Int, type: CmsMigratorType): CmsMigrationStatusSummary? {
        val migrator = getMigrator(key, type)
        if (migrator == null) {
            logger.info("No migration job found for $key of type ${type.name}")
            return null
        }

        return migrator.getStatusSummary()
    }

    fun getStatusAll(): CmsMigratorStatusAll {
        return CmsMigratorStatusAll(
            categories = categoryMigrators.values.map { it.getStatusSummary() },
            contents = contentMigrators.values.map { it.getStatusSummary() },
            versions = versionMigrators.values.map { it.getStatusSummary() }
        )
    }

    private fun removeInactive(migratorsMap: MigratorsMap): Int {
        return migratorsMap.keys.fold(0) { acc, key ->
            val migrator = migratorsMap[key]

            if (migrator?.state != CmsMigratorState.RUNNING) {
                migratorsMap.remove(key)
                return@fold acc + 1
            }

            return@fold acc
        }
    }

    fun cleanup(): Int {
        return removeInactive(categoryMigrators) +
                removeInactive(contentMigrators) +
                removeInactive(versionMigrators)
    }
}
