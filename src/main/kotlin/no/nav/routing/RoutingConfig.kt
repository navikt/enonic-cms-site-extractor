package no.nav.routing

import indexingRoutes
import no.nav.routing.routes.cms.cmsClientRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

        route("/indexing") {
            indexingRoutes()
        }

        route("/render") {
            install(CmsClientPlugin)

            get("/content/{contentKey}") {
                val contentKey = call.parameters["contentKey"]?.toInt()

                if (contentKey == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter contentKey must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)
                val result = client.renderContent(contentKey)

                call.respondText(result ?: "Oh noes")
            }

            get("/version/{versionKey}") {
                val versionKey = call.parameters["versionKey"]?.toInt()

                if (versionKey == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter versionKey must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)
                val result = client.renderVersion(versionKey)

                call.respondText(result ?: "Oh noes")
            }
        }
    }
}
