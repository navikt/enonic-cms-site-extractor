package no.nav.routing

import no.nav.routing.routes.cms.cmsClientRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.cms.renderer.ContentRenderer
import no.nav.routing.plugins.CmsClientPlugin
import no.nav.routing.plugins.getCmsClientFromCallContext
import no.nav.routing.routes.openSearch.openSearchRoutes

fun Application.configureRouting() {
    routing {
        route("/cms") {
            cmsClientRoutes()
        }

        route("/opensearch") {
            openSearchRoutes()
        }

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
