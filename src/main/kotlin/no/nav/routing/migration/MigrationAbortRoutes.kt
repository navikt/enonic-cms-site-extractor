package no.nav.routing.migration

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.migration.CmsMigratorHandler
import no.nav.migration.CmsMigratorType


private class Abort {
    @Resource("category/{categoryKey}")
    class Category(val categoryKey: Int)

    @Resource("content/{contentKey}")
    class Content(val contentKey: Int)

    @Resource("version/{versionKey}")
    class Version(val versionKey: Int)
}

private suspend fun abortReqHandler(
    key: Int,
    type: CmsMigratorType,
    call: ApplicationCall,
) {
    val result = CmsMigratorHandler.abortJob(key, type)
    if (!result) {
        call.respond("Could not abort migration job for ${type.name} $key - The job may not be running")
    } else {
        call.respond("Aborted migration job for ${type.name} $key")
    }
}

fun Route.migrationAbortRoutes() {
    get<Abort.Category> {
        abortReqHandler(
            it.categoryKey,
            CmsMigratorType.CATEGORY,
            call
        )
    }

    get<Abort.Content> {
        abortReqHandler(
            it.contentKey,
            CmsMigratorType.CONTENT,
            call
        )
    }

    get<Abort.Version> {
        abortReqHandler(
            it.versionKey,
            CmsMigratorType.VERSION,
            call
        )
    }
}
