package no.nav.migration

import CmsMigrationStatusSummary
import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.routing.CmsMigratorBuilder


private val logger = KtorSimpleLogger("CmsMigratorFactory")

object CmsMigratorHandler {
    private val migratorsByKey = mutableMapOf<Int, CmsMigrator>()
    private val migratorsByJobId = mutableMapOf<String, CmsMigrator>()

    private val waitingForInit = mutableSetOf<String>()

    fun getMigratorState(params: ICmsMigrationParams): String? {
        if (waitingForInit.contains(params.key.toString())) {
            return "initializing"
        }

        val migrator = migratorsByKey[params.key]
            ?: return null

        return "${migrator.state.name} - job id: ${migrator.jobId}"
    }

    suspend fun initByParams(
        params: ICmsMigrationParams,
        start: Boolean? = false,
        forceCreate: Boolean? = false,
        environment: ApplicationEnvironment?,
    ): Boolean {
        return init(params.key.toString(), start) {
            val key = params.key

            migratorsByKey[key]?.run {
                if (forceCreate != true) {
                    return@init this
                }

                this.abort()
                migratorsByKey.remove(key)
            }

            CmsMigratorBuilder().build(params, environment)
        }
    }

    suspend fun initByJobId(
        jobId: String,
        start: Boolean? = false,
        environment: ApplicationEnvironment?,
    ): Boolean {
        return init(jobId, start) {
            migratorsByJobId[jobId]
                ?: CmsMigratorBuilder().build(jobId, environment)
        }
    }

    private suspend fun init(
        initKey: String,
        start: Boolean? = false,
        migratorBuilder: suspend () -> CmsMigrator?
    ): Boolean {
        if (waitingForInit.contains(initKey)) {
            logger.info("Migrator for key $initKey is currently initializing")
            return false
        }

        waitingForInit.add(initKey)

        val migrator = try {
            migratorBuilder()
        } catch (e: Exception) {
            logger.error("Error while initalizing CMS migrator: ${e.message}")
            throw e
        } finally {
            waitingForInit.remove(initKey)
        }

        if (migrator == null) {
            return false
        }

        migratorsByKey[migrator.baseKey] = migrator
        migratorsByJobId[migrator.jobId] = migrator

        if (start == true) {
            migrator.run()
        }

        return true
    }

    suspend fun abortJob(jobId: String): Boolean {
        val migrator = migratorsByJobId[jobId]

        if (migrator == null) {
            logger.info("No migration job found for $jobId")
            return false
        }

        migrator.abort()

        return true
    }

    suspend fun abortAll(): List<String> {
        return migratorsByJobId.values.mapNotNull { migrator ->
            if (migrator.state != CmsMigratorState.RUNNING) {
                return@mapNotNull null
            }

            migrator.abort()

            migrator.jobId
        }
    }

    fun getStatus(jobId: String): CmsMigrationStatusSummary? {
        val migrator = migratorsByJobId[jobId]
        if (migrator == null) {
            logger.info("No migration job found for $jobId")
            return null
        }

        return migrator.getStatusSummary()
    }

    fun getStatusAll(): List<CmsMigrationStatusSummary> {
        return migratorsByJobId.values.map {
            it.getStatusSummary()
        }
    }

    fun cleanup(): List<String> {
        val migratorsToRemove = migratorsByJobId.values.filter {
            it.state != CmsMigratorState.RUNNING
        }.toSet()

        migratorsByJobId.values.removeAll(migratorsToRemove)
        migratorsByKey.values.removeAll(migratorsToRemove)

        return migratorsToRemove.map { it.jobId }
    }
}
