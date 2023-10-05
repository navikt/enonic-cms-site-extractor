package no.nav.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.cms.client.CmsClient
import no.nav.utils.parseAuthHeader

private val cmsClientKey = AttributeKey<CmsClient>("cmsClientKey")

fun getCmsClient(call: ApplicationCall): CmsClient? = call.attributes.getOrNull(cmsClientKey)

val CmsClientPlugin = createRouteScopedPlugin("CmsClient") {
    onCall { call ->
        val credential = parseAuthHeader(call.request)
        if (credential == null) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respondText("Missing or invalid authorization header")
            return@onCall
        }

        val url = call.request.queryParameters["url"]
        if (url == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respondText("Parameter url must be specified")
            return@onCall
        }

        val client = CmsClient(url, credential.name, credential.password)

        call.attributes.put(cmsClientKey, client)
    }
}