package no.nav.routing.internal

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import no.nav.migration.CmsMigratorHandler


private val logger = KtorSimpleLogger("InternalRoutes")

fun Route.internalRoutes() {
    get("/isAlive") {
        call.respondText("I am alive!")
    }

    get("/isReady") {
        call.respondText("I am ready!")
    }

    get("/stop") {
        logger.info("Received stop call, preparing to die!")

        CmsMigratorHandler.abortAll()

        delay(5000L)

        call.respond("Kill me!")
    }
}