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
    val host = environment?.config?.propertyOrNull("opensearch.host")?.getString()
    val port = environment?.config?.propertyOrNull("opensearch.port")?.getString()?.toInt()
    val user = environment?.config?.propertyOrNull("opensearch.user")?.getString()
    val password = environment?.config?.propertyOrNull("opensearch.password")?.getString()

    onCall { call ->
        if (host == null || port == null || user == null || password == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText("OpenSearch is not configured")
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