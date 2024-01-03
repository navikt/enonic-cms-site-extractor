package no.nav.routing.routes.openSearch

import com.jillesvangurp.ktsearch.RestException
import io.ktor.http.*
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

@Resource("index")
private class Index {
    @Resource("delete/{index}")
    class Delete(val parent: Index = Index(), val index: String)

    @Resource("get/{index}")
    class Get(val parent: Index = Index(), val index: String)
}

private suspend fun restExceptionHandler(call: ApplicationCall, ex: RestException) {
    call.response.status(HttpStatusCode(ex.response.status, ex.response.responseCategory.name))
    call.respondText(ex.response.text, ContentType.Application.Json)
}

fun Route.openSearchRoutes() {
    install(OpenSearchClientPlugin)
    install(ContentNegotiation) {
        json()
    }

    get<Info> {
        val info = getOpenSearchClientFromCallContext(call).info()
        call.respond(info)
    }

    get<Index.Delete> { params ->
        val result = getOpenSearchClientFromCallContext(call).deleteIndex(params.index)
        call.respond(result)
    }

    get<Index.Get> { params ->
        try {
            val result = getOpenSearchClientFromCallContext(call).getIndex(params.index)
            call.respond(result)
        } catch (ex: RestException) {
            restExceptionHandler(call, ex)
        }
    }
}
