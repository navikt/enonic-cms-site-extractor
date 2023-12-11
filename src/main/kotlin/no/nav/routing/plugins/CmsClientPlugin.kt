package no.nav.routing.plugins

import com.enonic.cms.api.client.ClientException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.cms.client.CmsClient
import no.nav.utils.parseAuthHeader

private val cmsClientKey = AttributeKey<CmsClient>("cmsClientKey")

fun getCmsClientFromCallContext(call: ApplicationCall): CmsClient = call.attributes.get(cmsClientKey)

val CmsClientPlugin = createRouteScopedPlugin("CmsClient") {
    onCall { call ->
        val credential = parseAuthHeader(call.request)
        if (credential == null) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respondText("Missing or invalid authorization header")
            return@onCall
        }

        val url = call.request.headers["cmsurl"]
        if (url == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respondText("Header \"cmsurl\" must be specified")
            return@onCall
        }

        val client = try {
            CmsClient(url, credential)
        } catch (e: ClientException) {
            null
        }

        if (client == null) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respondText("Failed to initialize CMS client for ${credential.name} to $url")
            return@onCall
        }

        call.attributes.put(cmsClientKey, client)
    }
}