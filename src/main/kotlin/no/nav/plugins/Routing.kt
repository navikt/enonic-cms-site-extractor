package no.nav.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.utils.documentToString

fun Application.configureRouting() {
    routing {
        get("/test") {
            val client = getCmsClient(call)

            if (client != null) {
                call.respondText("This should not happen!")
                return@get
            }

            call.respondText("Great success!")
        }

        route("/cms") {
            install(CmsClientPlugin)

            get("/content/{key}") {
                val key = call.parameters["key"]?.toInt()
                if (key == null) {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText("Parameter key must be a number")
                    return@get
                }

                val client = getCmsClient(call)

                val content = client?.getContent(key)

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

                val client = getCmsClient(call)

                val menu = client?.getMenu(key)

                val contentString = documentToString(menu)

                call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
            }
        }
    }
}
