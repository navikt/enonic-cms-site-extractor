package no.nav.utils

import io.ktor.server.application.*


fun getConfigVar(key: String, env: ApplicationEnvironment?): String? {
    return env?.config?.propertyOrNull(key)?.getString()
}