package no.nav.routing.migration

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.migration.*
import no.nav.utils.jsonResponse


private class Migrate {
    @Resource("category/{categoryKey}")
    class Category(
        val categoryKey: Int,
        val withChildren: Boolean? = false,
        val withContent: Boolean? = false,
        val withVersions: Boolean? = false
    )

    @Resource("content/{contentKey}")
    class Content(
        val contentKey: Int,
        val withVersions: Boolean? = false
    )

    @Resource("version/versionKey}")
    class Version(val versionKey: Int)
}

@Resource("abort")
private class Abort {
    @Resource("category/{categoryKey}")
    class Category(val parent: Abort = Abort(), val categoryKey: Int)

    @Resource("content/{contentKey}")
    class Content(val parent: Abort = Abort(), val contentKey: Int)

    @Resource("version/versionKey}")
    class Version(val parent: Abort = Abort(), val versionKey: Int)
}

@Resource("status")
private class Status {
    @Resource("category/{categoryKey}")
    class Category(val parent: Status = Status(), val categoryKey: Int)

    @Resource("content/{contentKey}")
    class Content(val parent: Status = Status(), val contentKey: Int)

    @Resource("version/versionKey}")
    class Version(val parent: Status = Status(), val versionKey: Int)
}

private suspend fun runMigrationHandler(
    migratorParams: CmsMigratorParams,
    call: ApplicationCall,
    environment: ApplicationEnvironment?
) {
    val migrator = CmsMigratorFactory
        .createOrRetrieveMigrator(
            migratorParams,
            environment
        )

    if (migrator == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        call.respond("Could not initialize CMS migrator")
        return
    }

    if (migrator.getStatus().state == CmsMigratorState.RUNNING) {
        call.response.status(HttpStatusCode.Forbidden)
        call.respond("Migrator for ${migrator.params.key} is already running")
        return
    }

    val response = migrator.run()
    call.respond(response)
}

private suspend fun abortHandler(
    key: Int,
    type: CmsMigratorType,
    call: ApplicationCall,
) {
    val result = CmsMigratorFactory.abortJob(key, type)
    if (!result) {
        call.respond("Could not abort migration job for ${type.name} ${key} - The job may not be running")
    } else {
        call.respond("Aborted migration job for ${type.name} ${key}")
    }
}

private suspend fun getStatusHandler(
    key: Int,
    type: CmsMigratorType,
    call: ApplicationCall,
) {
    val result = CmsMigratorFactory.getStatus(key, type)
    if (result == null) {
        call.respond("No migration job found for ${type.name} ${key} - The job may not be running")
    } else {
        jsonResponse(call, result, "Failed to get status for ${type.name} $key")
    }
}

fun Route.migrationRoutes() {
    install(ContentNegotiation) {
        json()
    }

    get<Migrate.Category> {
        runMigrationHandler(
            CmsCategoryMigratorParams(
                key = it.categoryKey,
                withChildren = it.withChildren,
                withContent = it.withContent,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment
        )
    }

    get<Migrate.Content> {
        runMigrationHandler(
            CmsContentMigratorParams(
                key = it.contentKey,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment
        )
    }

    get<Migrate.Version> {
        runMigrationHandler(
            CmsVersionMigratorParams(it.versionKey),
            call,
            this@migrationRoutes.environment
        )
    }

    get<Abort.Category> {
        abortHandler(it.categoryKey, CmsMigratorType.CATEGORY, call)
    }

    get<Abort.Content> {
        abortHandler(it.contentKey, CmsMigratorType.CONTENT, call)
    }

    get<Abort.Version> {
        abortHandler(it.versionKey, CmsMigratorType.VERSION, call)
    }

    get<Status.Category> {
        getStatusHandler(it.categoryKey, CmsMigratorType.CATEGORY, call)
    }

    get<Status.Content> {
        getStatusHandler(it.contentKey, CmsMigratorType.CONTENT, call)
    }

    get<Status.Version> {
        getStatusHandler(it.versionKey, CmsMigratorType.VERSION, call)
    }
}