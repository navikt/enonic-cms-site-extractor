package no.nav

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import no.nav.routing.configureRouting


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(IgnoreTrailingSlash)

    install(CallLogging) {
        filter { call -> !call.request.path().startsWith("/internal") }
    }

    install(Resources)

    configureRouting()
}
