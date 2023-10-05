package no.nav.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import no.nav.cms.client.CmsClient
import no.nav.utils.parseAuthHeader

private val cmsClientKey = io.ktor.util.AttributeKey<CmsClient>("cmsClientKey")

fun getCmsClient(call: ApplicationCall) = call.attributes[cmsClientKey]

val CmsClientPlugin = createApplicationPlugin("CmsClient") {

    onCall { call ->
        val credendial = parseAuthHeader(call.request)
        if (credendial == null) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respondText("Missing or invalid authorization header")
            return@onCall
        }

        val url  = call.request.queryParameters["url"]
        if (url == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respondText("Parameter url must be specified")
            return@onCall
        }

        val client = CmsClient(url, credendial.name, credendial.password)

        call.attributes.put(cmsClientKey, client)
    }
}