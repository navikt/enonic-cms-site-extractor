package no.nav.routing.routes.openSearch

import com.jillesvangurp.ktsearch.RestException
import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.routing.plugins.OpenSearchKtClientPlugin
import no.nav.routing.plugins.getOpenSearchKtClientFromCallContext

@Resource("info")
private class Info

@Resource("index")
private class Index {
    @Resource("create")
    class Create(val parent: Index = Index(), val index: String)

    @Resource("delete")
    class Delete(val parent: Index = Index(), val index: String)

    @Resource("get")
    class Get(val parent: Index = Index(), val index: String)
}

private suspend fun restExceptionHandler(call: ApplicationCall, ex: RestException) {
    call.response.status(HttpStatusCode(ex.response.status, ex.response.responseCategory.name))
    call.respondText(ex.response.text, ContentType.Application.Json)
}

fun Route.openSearchRoutes() {
    install(OpenSearchKtClientPlugin)
    install(ContentNegotiation) {
        json()
    }

    get<Info> {
        val info = getOpenSearchKtClientFromCallContext(call).info()
        call.respond(info)
    }

    get<Index.Create> { params ->
        val result = getOpenSearchKtClientFromCallContext(call).createIndexIfNotExist(params.index)
        call.respondText("Created: $result")
    }

    get<Index.Delete> { params ->
        val result = getOpenSearchKtClientFromCallContext(call).deleteIndex(params.index)
        call.respond(result)
    }

    get<Index.Get> { params ->
        try {
            val result = getOpenSearchKtClientFromCallContext(call).getIndex(params.index)
            call.respond(result)
        } catch (ex: RestException) {
            restExceptionHandler(call, ex)
        }
    }
}