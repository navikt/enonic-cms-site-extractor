package no.nav.routing.migration

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
}

@Resource("cleanup")
private class Cleanup

private suspend fun migrationReqHandler(
    migrationParams: ICmsMigrationParams,
    call: ApplicationCall,
    environment: ApplicationEnvironment?,
    start: Boolean? = false,
    forceCreate: Boolean? = false
) {
    val migratorState = CmsMigratorHandler.getMigratorState(migrationParams)

    val msg = if (migratorState == null) {
        "No migrator found for ${migrationParams.key}, it will be created"
    } else {
        "Current migrator state: $migratorState"
    }

    call.respond(msg)

    CmsMigratorHandler
        .initMigrator(
            migrationParams,
            environment,
            start,
            forceCreate,
        )
}

fun Route.migrationRoutes() {
    install(ContentNegotiation) {
        json()
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

    get<Cleanup> {
        val numItemsRemoved = CmsMigratorHandler.cleanup()
        call.respond("Removed $numItemsRemoved inactive migrator instances")
    }

    route("status") {
        migrationStatusRoutes()
    }

    route("abort") {
        migrationAbortRoutes()
    }
}