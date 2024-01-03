package no.nav.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


fun parseDateTime(datetime: String?): String? {
    if (datetime == null) {
        return null
    }

    return try {
        LocalDateTime
            .parse(datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss][.S][S][S]"))
            .toString()
    } catch (e: DateTimeParseException) {
        return null
    }
}
