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

    println("Encoded: $encodedAuth")

    val decodedAuth = String(Base64.getDecoder().decode(encodedAuth))

    println("Decoded: $decodedAuth")

    val (username, password) = decodedAuth.split(":")

    println("User: $username - pw: $password")

    return UserPasswordCredential(username, password)
}