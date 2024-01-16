package no.nav.routing.migration

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.migration.CmsMigratorHandler


private class Status {
    @Resource("job/{jobId}")
    class Job(val jobId: String)

    @Resource("all")
    class All
}

fun Route.migrationStatusRoutes() {
    get<Status.Job> {
        val result = CmsMigratorHandler.getStatus(it.jobId)
        if (result == null) {
            call.respond("No migration status found for ${it.jobId} - The job may not have been initialized")
        } else {
            call.respond(result)
        }
    }

    get<Status.All> {
        val statusAll = CmsMigratorHandler.getStatusAll()
        if (statusAll.isEmpty()) {
            return@get call.respond("No migration jobs found")
        }

        call.respond(statusAll)
    }
}