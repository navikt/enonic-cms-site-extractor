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
        val statusOnly: Boolean? = false
    )

    @Resource("content/{contentKey}")
    class Content(
        val contentKey: Int,
        val withVersions: Boolean? = false,
        val statusOnly: Boolean? = false
    )

    @Resource("version/{versionKey}")
    class Version(
        val versionKey: Int,
        val statusOnly: Boolean? = false
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

private suspend fun runMigrationHandler(
    migratorParams: ICmsMigrationParams,
    call: ApplicationCall,
    environment: ApplicationEnvironment?,
    statusOnly: Boolean? = false
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

    val response = migrator.status

    if (statusOnly != true) {
        migrator.run()
    }

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
        runMigrationHandler(
            CmsCategoryMigrationParams(
                key = it.categoryKey,
                withChildren = it.withChildren,
                withContent = it.withContent,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment,
            it.statusOnly
        )
    }

    get<Migrate.Content> {
        runMigrationHandler(
            CmsContentMigrationParams(
                key = it.contentKey,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment,
            it.statusOnly
        )
    }

    get<Migrate.Version> {
        runMigrationHandler(
            CmsVersionMigrationParams(it.versionKey),
            call,
            this@migrationRoutes.environment,
            it.statusOnly
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