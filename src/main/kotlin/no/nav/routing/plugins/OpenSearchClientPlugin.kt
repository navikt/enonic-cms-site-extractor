package no.nav.routing.plugins

import no.nav.db.openSearch.OpenSearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.db.openSearch.OpenSearchClientBuilder
import no.nav.utils.parseAuthHeader


private val openSearchClientKey = AttributeKey<OpenSearchClient>("openSearchClientKey")

fun getOpenSearchClientFromCallContext(call: ApplicationCall): OpenSearchClient =
    call.attributes[openSearchClientKey]

val OpenSearchClientPlugin = createRouteScopedPlugin("OpenSearchClient") {
    onCall { call ->
        val credential = parseAuthHeader(call.request)
        if (credential == null) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respondText("Missing or invalid authorization header")
            return@onCall
        }

        val client = OpenSearchClientBuilder(
            "opensearch-personbruker-enonic-cms-archive-nav-prod.a.aivencloud.com",
            26482,
            credential
        ).build()

        call.attributes.put(openSearchClientKey, client)
    }
}