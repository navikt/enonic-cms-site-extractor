package no.nav.utils

import org.jdom.Document
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import java.io.IOException
import java.io.StringWriter

@Throws(RuntimeException::class)
fun documentToString(doc: Document?): String? {
    val sw = StringWriter()
    val outputter = XMLOutputter(Format.getPrettyFormat())
    return try {
        outputter.output(doc, sw)
        sw.buffer.toString()
    } catch (e: IOException) {
        throw RuntimeException("Failed to print document", e)
    }
}
