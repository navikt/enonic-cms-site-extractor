package no.nav.utils

import io.ktor.util.logging.*
import org.jdom.Document
import org.jdom.Element
import org.jdom.Parent
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import java.io.IOException


private val logger = KtorSimpleLogger("XmlToString")

@Throws(RuntimeException::class)
private fun documentToString(document: Document?): String? {
    return try {
        XMLOutputter(Format.getPrettyFormat()).outputString(document)
    } catch (e: IOException) {
        throw RuntimeException("Failed to print document", e)
    }
}

@Throws(RuntimeException::class)
private fun elementToString(element: Element?): String? {
    return try {
        XMLOutputter(Format.getPrettyFormat()).outputString(element)
    } catch (e: IOException) {
        throw RuntimeException("Failed to print element", e)
    }
}

fun <T : Parent> xmlToString(xml: T): String? {
    return when (xml) {
        is Document -> documentToString(xml)
        is Element -> elementToString(xml)
        else -> {
            logger.error("Unsupported XML type: ${xml::class}")
            return null
        }
    }
}