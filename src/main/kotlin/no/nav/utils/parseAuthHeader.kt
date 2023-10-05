package no.nav.utils

import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import java.util.*

fun parseAuthHeader(request: ApplicationRequest): UserPasswordCredential? {
    val parsedAuth = parseAuthorizationHeader(request.headers[HttpHeaders.Authorization] ?: "")
    if (parsedAuth !is HttpAuthHeader.Single || parsedAuth.authScheme != AuthScheme.Basic) {
        return null
    }

    val encodedAuth = parsedAuth.blob.removePrefix("${AuthScheme.Basic} ")
    val decodedAuth = String(Base64.getDecoder().decode(encodedAuth))
    val (username, password) = decodedAuth.split(":")

    return UserPasswordCredential(username, password)
}