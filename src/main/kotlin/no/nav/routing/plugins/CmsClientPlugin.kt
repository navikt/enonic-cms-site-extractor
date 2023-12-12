package no.nav.routing.plugins

import com.enonic.cms.api.client.ClientException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.cms.client.CmsClient

private val cmsClientKey = AttributeKey<CmsClient>("cmsClientKey")

fun getCmsClientFromCallContext(call: ApplicationCall): CmsClient = call.attributes.get(cmsClientKey)

val CmsClientPlugin = createRouteScopedPlugin("CmsClient") {
    val url = environment?.config?.propertyOrNull("cms.url")?.getString()
    val user = environment?.config?.propertyOrNull("cms.user")?.getString()
    val password = environment?.config?.propertyOrNull("cms.password")?.getString()

    onCall { call ->
        if (url == null || user == null || password == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText("CMS service parameters not found")
            return@onCall
        }

        val client = try {
            CmsClient(url, UserPasswordCredential(user, password))
        } catch (e: ClientException) {
            null
        }

        if (client == null) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respondText("Failed to initialize CMS client for $user to $url")
            return@onCall
        }

        call.attributes.put(cmsClientKey, client)
    }
}
