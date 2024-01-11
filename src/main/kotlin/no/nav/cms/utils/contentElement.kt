package no.nav.cms.utils

import org.jdom.Document
import org.jdom.Element


private const val CONTENT_ELEMENT_NAME = "content"
private const val CATEGORY_ELEMENT_NAME = "category"

fun getContentElement(document: Document): Element? {
    return document
        .rootElement
        ?.getChild(CONTENT_ELEMENT_NAME)
}

fun getCategoryElement(document: Document): Element? {
    return document
        .rootElement
        ?.getChild(CATEGORY_ELEMENT_NAME)
}

fun getChildElements(element: Element, name: String): List<Element>? {
    return element
        .getChildren(name)
        ?.filterIsInstance<Element>()
}