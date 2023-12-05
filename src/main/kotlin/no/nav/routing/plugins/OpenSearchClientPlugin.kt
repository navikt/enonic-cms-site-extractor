package no.nav.routing.plugins

import no.nav.db.openSearch.OpenSearchKtClientWrapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.db.openSearch.OpenSearchClientBuilder
import no.nav.db.openSearch.OpenSearchJavaClientWrapper
import no.nav.utils.parseAuthHeader

private val openSearchJavaClientKey = AttributeKey<OpenSearchJavaClientWrapper>("openSearchJavaClientKey")
private val openSearchKtClientKey = AttributeKey<OpenSearchKtClientWrapper>("openSearchKtClientKey")

fun getOpenSearchJavaClientFromCallContext(call: ApplicationCall): OpenSearchJavaClientWrapper =
    call.attributes[openSearchJavaClientKey]

fun getOpenSearchKtClientFromCallContext(call: ApplicationCall): OpenSearchKtClientWrapper =
    call.attributes[openSearchKtClientKey]

val OpenSearchJavaClientPlugin = createRouteScopedPlugin("OpenSearchClient") {
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
        ).buildJavaClient()

        val clientWrapper = OpenSearchJavaClientWrapper(client)

        call.attributes.put(openSearchJavaClientKey, clientWrapper)
    }
}

val OpenSearchKtClientPlugin = createRouteScopedPlugin("OpenSearchKtClient") {
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
        ).buildKtClient()

        val clientWrapper = OpenSearchKtClientWrapper(client)

        call.attributes.put(openSearchKtClientKey, clientWrapper)
    }
}