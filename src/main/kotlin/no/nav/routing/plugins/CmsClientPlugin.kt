package no.nav.routing.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import no.nav.cms.client.CmsClient
import no.nav.cms.client.CmsClientBuilder


private val cmsClientKey = AttributeKey<CmsClient>("cmsClientKey")

fun getCmsClientFromCallContext(call: ApplicationCall): CmsClient = call.attributes[cmsClientKey]

val CmsClientPlugin = createRouteScopedPlugin("CmsClient") {
    on(AuthenticationChecked) { call ->
        if (call.principal<UserIdPrincipal>()?.name == null) {
            return@on
        }

        val client = CmsClientBuilder(environment).build()

        if (client == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText("Failed to initialize CMS client")
            return@on
        }

        call.attributes.put(cmsClientKey, client)
    }
}
