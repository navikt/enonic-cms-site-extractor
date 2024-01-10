package no.nav.utils

import io.ktor.util.logging.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


private val logger = KtorSimpleLogger("parseDateTime")

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss][.S][S][S]")

fun parseDateTime(datetime: String?): String? {
    if (datetime == null || datetime == "") {
        return null
    }

    return try {
        LocalDateTime
            .parse(datetime, formatter)
            .toString()
    } catch (e: DateTimeParseException) {
        logger.error("Failed to parse datetime string \"$datetime\" - ${e.message}")
        return null
    }
}

fun withTimestamp(msg: String): String {
    val timestamp = LocalDateTime
        .now()
        .format(formatter)

    return "[$timestamp] $msg"
}