package no.nav.routing.routes.openSearch

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.routing.plugins.OpenSearchClientPlugin
import no.nav.routing.plugins.getOpenSearchClientFromCallContext

@Resource("ping")
private class Ping

@Resource("createIndex")
private class CreateIndex(val index: String)

@Resource("deleteIndex")
private class DeleteIndex(val index: String)

fun Route.openSearchRoutes() {
    install(OpenSearchClientPlugin)

    get<Ping> {
        val ping = getOpenSearchClientFromCallContext(call).ping()

        call.respondText("Ping: $ping")
    }

    get<CreateIndex> { params ->
        val result = getOpenSearchClientFromCallContext(call).createIndexIfNotExist(params.index)

        call.respondText("Created: $result")
    }

    get<DeleteIndex> { params ->
        val result = getOpenSearchClientFromCallContext(call).deleteIndex(params.index)

        call.respondText("Deleted: $result")
    }

}