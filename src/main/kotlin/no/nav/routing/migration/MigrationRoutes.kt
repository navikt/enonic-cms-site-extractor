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
    @Resource("content/{contentKey}")
    class Content(
        val contentKey: Int,
        val withVersions: Boolean? = false
    )

    @Resource("version/versionKey}")
    class Version(val versionKey: Int)

    @Resource("category/{categoryKey}")
    class Category(
        val categoryKey: Int,
        val withChildren: Boolean? = false,
        val withContent: Boolean? = false,
        val withVersions: Boolean? = false
    )
}

private suspend fun migrationRequestHandler(
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

    if (migrator.state == CmsMigratorState.RUNNING) {
        call.response.status(HttpStatusCode.Forbidden)
        call.respond("Migrator for ${migrator.params.key} is already running")
        return
    }

    val response = migrator.run()
    call.respond(response)
}

fun Route.migrationRoutes() {
    install(ContentNegotiation) {
        json()
    }

    get<Migrate.Category> {
        migrationRequestHandler(
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
        migrationRequestHandler(
            CmsContentMigratorParams(
                key = it.contentKey,
                withVersions = it.withVersions
            ),
            call,
            this@migrationRoutes.environment
        )
    }

    get<Migrate.Version> {
        migrationRequestHandler(
            CmsVersionMigratorParams(it.versionKey),
            call,
            this@migrationRoutes.environment
        )
    }
}