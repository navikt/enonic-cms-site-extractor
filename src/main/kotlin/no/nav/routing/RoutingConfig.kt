package no.nav.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.cms.client.CmsRestClient
import no.nav.cms.renderer.ContentRenderer
import no.nav.utils.documentToString
import no.nav.utils.parseAuthHeader

fun Application.configureRouting() {
    routing {
        route("/cms") {
            install(CmsClientPlugin)

            get("/content/{key}") {
                val key = call.parameters["key"]?.toInt()
                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be a number")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val content = client.getContent(key)

                val contentString = documentToString(content)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/version/{key}") {
                val key = call.parameters["key"]?.toInt()
                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be a number")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val content = client.getContentVersion(key)

                val contentString = documentToString(content)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/menu/{key}") {
                val key = call.parameters["key"]?.toInt()

                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val menu = client.getMenu(key)

                val contentString = documentToString(menu)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/menuitem/{key}") {
                val key = call.parameters["key"]?.toInt()

                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val menu = client.getMenuItem(key)

                val contentString = documentToString(menu)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/categories/{key}") {
                val key = call.parameters["key"]?.toInt()

                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be specified")
                    return@get
                }

                val depth = call.request.queryParameters["depth"]?.toInt()

                val client = getCmsClientFromCallContext(call)

                val menu = client.getCategories(key, depth)

                val contentString = documentToString(menu)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/contentByCategory/{key}") {
                val key = call.parameters["key"]?.toInt()

                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val menu = client.getContentByCategory(key)

                val contentString = documentToString(menu)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/menudata/{key}") {
                val key = call.parameters["key"]?.toInt()

                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be specified")
                    return@get
                }

                val client = getCmsClientFromCallContext(call)

                val menu = client.getMenuData(key)

                val contentString = documentToString(menu)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }

            get("/render/{contentKey}") {
                val contentKey = call.parameters["contentKey"]?.toInt()

                if (contentKey == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter contentKey must be specified")
                    return@get
                }

                val credential = parseAuthHeader(call.request)
                if (credential == null) {
                    call.response.status(HttpStatusCode.Unauthorized)
                    call.respondText("Missing or invalid authorization header")
                    return@get
                }

                val url = call.request.headers["cmsurl"]
                if (url == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Header \"cmsurl\" must be specified")
                    return@get
                }

                val rpcClient = getCmsClientFromCallContext(call)
                val restClient = CmsRestClient(url, credential.name, credential.password)

                val contentRenderer = ContentRenderer(contentKey, rpcClient, restClient)

                val result = contentRenderer.render()

                call.respondText(result ?: "Oh noes")
            }
        }
    }
}
