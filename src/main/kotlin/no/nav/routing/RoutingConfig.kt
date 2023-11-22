package no.nav.routing

import no.nav.routing.routes.cmsClient.cmsClientRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import no.nav.cms.renderer.ContentRenderer
import no.nav.openSearch.OpenSearchClientBuilder
import no.nav.utils.parseAuthHeader

private val logger = KtorSimpleLogger("RouteConfig")

fun Application.configureRouting() {
    routing {
        route("/cms") {
            cmsClientRoutes()
        }

        route("/opensearch") {
            get("test") {
                val credentials = parseAuthHeader(call.request)
                if (credentials == null) {
                    call.respondText("Oh noes")
                    return@get
                }

                val openSearchClient = OpenSearchClientBuilder(
                    "opensearch-personbruker-enonic-cms-archive-nav-prod.a.aivencloud.com",
                    26482,
                    credentials
                ).build()

                val ping = openSearchClient.ping().value()

                call.respondText("Hello - Ping: $ping")
            }
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
