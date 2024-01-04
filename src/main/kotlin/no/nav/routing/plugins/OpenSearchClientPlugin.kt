package no.nav.routing.plugins

import no.nav.db.openSearch.OpenSearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.db.openSearch.OpenSearchClientBuilder
import no.nav.utils.getConfigVar


private val openSearchClientKey = AttributeKey<OpenSearchClient>("openSearchClientKey")

fun getOpenSearchClientFromCallContext(call: ApplicationCall): OpenSearchClient =
    call.attributes[openSearchClientKey]

val OpenSearchClientPlugin = createRouteScopedPlugin("OpenSearchClient") {
    val host = getConfigVar("opensearch.host", environment)
    val port = getConfigVar("opensearch.port", environment)?.toInt()
    val user = getConfigVar("opensearch.user", environment)
    val password = getConfigVar("opensearch.password", environment)

    onCall { call ->
        if (host == null || port == null || user == null || password == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText("OpenSearch service parameters not found")
            return@onCall
        }

        if (call.principal<UserIdPrincipal>() == null) {
            return@onCall
        }

        val client = OpenSearchClientBuilder(
            host,
            port,
            UserPasswordCredential(user, password)
        ).build()

        call.attributes.put(openSearchClientKey, client)
    }
}