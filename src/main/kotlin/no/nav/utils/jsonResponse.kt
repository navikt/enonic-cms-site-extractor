package no.nav.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


suspend inline fun <reified T : Any> jsonResponse(call: ApplicationCall, json: T?, nullMsg: String) {
    if (json == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        return call.respond(nullMsg)
    }

    try {
        val jsonString = Json.encodeToString(json)
        call.respondText(jsonString, ContentType.Application.Json)
    } catch (e: Exception) {
        call.response.status(HttpStatusCode.InternalServerError)
        call.respond("Failed to serialize response to JSON - ${e.message}")
    }
}