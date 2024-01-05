package no.nav.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jdom.Parent


suspend fun <T : Parent> xmlResponse(call: ApplicationCall, xml: T?) {
    if (xml == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        return call.respondText("No XML provided")
    }

    val documentString = xmlToString(xml)

    if (documentString == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        return call.respondText("Failed to parse XML")
    }

    call.respondText(documentString, ContentType.Text.Xml)
}
