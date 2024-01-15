package no.nav.routing.migration

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.migration.CmsMigratorHandler
import no.nav.migration.CmsMigratorType


private class Status {
    @Resource("category/{categoryKey}")
    class Category(
        val categoryKey: Int,
        val withResults: Boolean = false,
        val withRemaining: Boolean = false
    )

    @Resource("content/{contentKey}")
    class Content(
        val contentKey: Int,
        val withResults: Boolean = false,
        val withRemaining: Boolean = false
    )

    @Resource("version/{versionKey}")
    class Version(
        val versionKey: Int,
        val withResults: Boolean = false,
        val withRemaining: Boolean = false
    )

    @Resource("all")
    class All()
}

private suspend fun statusReqHandler(
    key: Int,
    type: CmsMigratorType,
    call: ApplicationCall,
    withResults: Boolean?,
    withRemaining: Boolean?
) {
    val result = CmsMigratorHandler.getStatus(key, type, withResults, withRemaining)
    if (result == null) {
        call.respond("No migration status found for ${type.name} $key - The job may not have been initialized")
    } else {
        call.respond(result)
    }
}

fun Route.migrationStatusRoutes() {
    get<Status.Category> {
        statusReqHandler(
            it.categoryKey,
            CmsMigratorType.CATEGORY,
            call,
            it.withResults,
            it.withRemaining
        )
    }

    get<Status.Content> {
        statusReqHandler(
            it.contentKey,
            CmsMigratorType.CONTENT,
            call,
            it.withResults,
            it.withRemaining
        )
    }

    get<Status.Version> {
        statusReqHandler(
            it.versionKey,
            CmsMigratorType.VERSION,
            call,
            it.withResults,
            it.withRemaining
        )
    }

    get<Status.All> {
        val statusAll = CmsMigratorHandler.getStatusAll()
        call.respond(statusAll)
    }
}