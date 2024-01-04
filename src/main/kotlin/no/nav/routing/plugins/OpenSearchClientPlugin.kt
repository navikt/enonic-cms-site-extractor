package no.nav.routing.plugins

import no.nav.db.openSearch.OpenSearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.db.openSearch.OpenSearchClientBuilder


private val openSearchClientKey = AttributeKey<OpenSearchClient>("openSearchClientKey")

fun getOpenSearchClientFromCallContext(call: ApplicationCall): OpenSearchClient =
    call.attributes[openSearchClientKey]

val OpenSearchClientPlugin = createRouteScopedPlugin("OpenSearchClient") {
    on(AuthenticationChecked) { call ->
        if (call.principal<UserIdPrincipal>()?.name == null) {
            return@on
        }

        val client = OpenSearchClientBuilder(environment).build()

        if (client == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText("Failed to initialize OpenSearch client")
            return@on
        }

        call.attributes.put(openSearchClientKey, client)
    }
}