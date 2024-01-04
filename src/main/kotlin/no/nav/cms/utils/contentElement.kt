package no.nav.cms.utils

import io.ktor.util.logging.*
import org.jdom.Document
import org.jdom.Element


private const val CONTENT_ELEMENT_NAME = "content"

private val logger = KtorSimpleLogger("getContentElement")

fun getContentElement(document: Document): Element? {
    val contentElement = document
        .rootElement
        ?.getChild("content")

    val elementName = contentElement?.name

    if (elementName != CONTENT_ELEMENT_NAME) {
        logger.error("Element is not a valid content element (expected $CONTENT_ELEMENT_NAME - got $elementName")
        return null
    }

    return contentElement
}