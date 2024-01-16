package no.nav.routing.migration

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.migration.CmsMigratorHandler


private class Abort {
    @Resource("job/{jobId}")
    class Job(val jobId: String)

    @Resource("all")
    class All
}

fun Route.migrationAbortRoutes() {
    get<Abort.Job> {
        val didAbort = CmsMigratorHandler.abortJob(it.jobId)
        if (!didAbort) {
            call.respond("Could not abort migration job for ${it.jobId} - The job may not be running")
        } else {
            call.respond("Aborted migration job for ${it.jobId}")
        }
    }

    get<Abort.All> {
        val abortedJobs = CmsMigratorHandler.abortAll()
        call.respond("Aborted ${abortedJobs.size} jobs: ${abortedJobs.joinToString(", ")}")
    }
}
