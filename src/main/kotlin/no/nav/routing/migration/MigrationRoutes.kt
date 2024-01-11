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


private class Migrate {
    @Resource("category/{categoryKey}")
    class Category(
        val categoryKey: Int,
        val withChildren: Boolean? = false,
        val withContent: Boolean? = false,
        val withVersions: Boolean? = false,
        val start: Boolean? = false,
        val restart: Boolean? = false
    )

    @Resource("content/{contentKey}")
    class Content(
        val contentKey: Int,
        val withVersions: Boolean? = false,
        val start: Boolean? = false,
        val restart: Boolean? = false
    )

    @Resource("version/{versionKey}")
    class Version(
        val versionKey: Int,
        val start: Boolean? = false,
        val restart: Boolean? = false
    )
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

private suspend fun migrationHandler(
    migrationParams: ICmsMigrationParams,
    call: ApplicationCall,
    environment: ApplicationEnvironment?,
    start: Boolean? = false,
    forceCreate: Boolean? = false
) {
    val migrator = CmsMigratorFactory
        .createOrRetrieveMigrator(
            migrationParams,
            environment,
            forceCreate
        )

    if (migrator == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        call.respond("Failed to initialize CMS migrator!")
        return
    }

    if (start == true) {
        migrator.run()
    }

    val response = migrator.getStatus()

    call.respond(response)
}

private suspend fun abortHandler(
    key: Int,
    type: CmsMigratorType,
    call: ApplicationCall,
) {
    val result = CmsMigratorFactory.abortJob(key, type)
    if (!result) {
        call.respond("Could not abort migration job for ${type.name} $key - The job may not be running")
    } else {
        call.respond("Aborted migration job for ${type.name} $key")
    }
}

fun Route.migrationRoutes() {
    install(ContentNegotiation) {
        json()
    }

    get<Migrate.Category> {
        migrationHandler(
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
        migrationHandler(
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
        migrationHandler(
            CmsVersionMigrationParams(it.versionKey),
            call,
            this@migrationRoutes.environment,
            it.start,
            it.restart
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
}