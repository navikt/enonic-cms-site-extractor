package no.nav.routing.routes.openSearch

import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.routing.plugins.OpenSearchClientPlugin
import no.nav.routing.plugins.getOpenSearchClientFromCallContext


@Resource("info")
private class Info()

fun Route.openSearchRoutes() {
    install(OpenSearchClientPlugin)
    install(ContentNegotiation) {
        json()
    }

    get<Info> {
        val info = getOpenSearchClientFromCallContext(call).info()
        call.respond(info)
    }
}
