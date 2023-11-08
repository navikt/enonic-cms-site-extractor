package no.nav.routing

import no.nav.routing.routes.cmsClient.cmsClientRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.cms.renderer.ContentRenderer


fun Application.configureRouting() {
    routing {
        cmsClientRoutes()

        route("/render") {
            install(CmsClientPlugin)
            get("/{contentKey}") {
                val contentKey = call.parameters["contentKey"]?.toInt()

                if (contentKey == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter contentKey must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val contentRenderer = ContentRenderer(contentKey, client)

                val result = contentRenderer.render()

                call.respondText(result ?: "Oh noes")
            }
        }
    }
}
