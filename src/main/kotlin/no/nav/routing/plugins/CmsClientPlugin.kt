package no.nav.routing.plugins

import com.enonic.cms.api.client.ClientException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.cms.client.CmsClient
import no.nav.utils.getConfigVar


private val cmsClientKey = AttributeKey<CmsClient>("cmsClientKey")

fun getCmsClientFromCallContext(call: ApplicationCall): CmsClient = call.attributes[cmsClientKey]

val CmsClientPlugin = createRouteScopedPlugin("CmsClient") {
    val url = getConfigVar("cms.url", environment)
    val user = getConfigVar("cms.user", environment)
    val password = getConfigVar("cms.password", environment)

    onCall { call ->
        if (url == null || user == null || password == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText("CMS service parameters not found")
            return@onCall
        }

        if (call.principal<UserIdPrincipal>() == null) {
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
