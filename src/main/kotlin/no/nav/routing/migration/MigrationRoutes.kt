package no.nav.routing.migration

import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.migration.*
import no.nav.openSearch.OpenSearchClientBuilder


private class Migrate {
    @Resource("category/{categoryKey}")
    class Category(
        val categoryKey: Int,
        val withChildren: Boolean = false,
        val withContent: Boolean = false,
        val withVersions: Boolean = false,
        val start: Boolean = false,
        val restart: Boolean = false
    )

    @Resource("content/{contentKey}")
    class Content(
        val contentKey: Int,
        val withVersions: Boolean = false,
        val start: Boolean = false,
        val restart: Boolean = false
    )

    @Resource("version/{versionKey}")
    class Version(
        val versionKey: Int,
        val start: Boolean = false,
        val restart: Boolean = false
    )

    @Resource("resume/{jobId}")
    class Resume(val jobId: String, val start: Boolean = false)
}

@Resource("cleanup")
private class Cleanup

@Resource("job/{jobId}")
class Job(val jobId: String)

private suspend fun migrationReqHandler(
    migrationParams: ICmsMigrationParams,
    call: ApplicationCall,
    environment: ApplicationEnvironment?,
    start: Boolean? = false,
    forceCreate: Boolean? = false
) {
    val migratorState = CmsMigratorHandler.getMigratorState(migrationParams)

    val msg = if (migratorState == null) {
        "Creating migration job for ${migrationParams.key}"
    } else {
        "Migration job found for ${migrationParams.key} - current state: $migratorState"
    }

    call.respond(msg)

    CmsMigratorHandler
        .initByParams(
            migrationParams,
            start,
            forceCreate,
            environment,
        )
}

fun Route.migrationRoutes() {
    install(ContentNegotiation) {
        json()
    }

    get<Job> {
        val jobStatus = OpenSearchClientBuilder(this@migrationRoutes.environment)
            .build()
            ?.getMigrationStatus(it.jobId)

        call.respond(jobStatus ?: "Oh noes")
    }

    get<Migrate.Category> {
        migrationReqHandler(
            CmsCategoryMigrationParams(
                key = it.categoryKey,
                withChildren = it.withChildren,
                withContent = it.withContent,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment,
            it.start,
            it.restart
        )
    }

    get<Migrate.Content> {
        migrationReqHandler(
            CmsContentMigrationParams(
                key = it.contentKey,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment,
            it.start,
            it.restart
        )
    }

    get<Migrate.Version> {
        migrationReqHandler(
            CmsVersionMigrationParams(it.versionKey),
            call,
            this@migrationRoutes.environment,
            it.start,
            it.restart
        )
    }

    get<Migrate.Resume> {
        val didResume = CmsMigratorHandler
            .initByJobId(it.jobId, it.start, this@migrationRoutes.environment)

        val response = if (didResume) {
            "Resumed job with id ${it.jobId}"
        } else {
            "Could not resume job with id ${it.jobId}"
        }

        call.respond(response)
    }

    get<Cleanup> {
        val jobsRemoved = CmsMigratorHandler.cleanup()
        call.respond("Removed inactive jobs: ${jobsRemoved.joinToString(", ")}")
    }

    route("status") {
        migrationStatusRoutes()
    }

    route("abort") {
        migrationAbortRoutes()
    }
}