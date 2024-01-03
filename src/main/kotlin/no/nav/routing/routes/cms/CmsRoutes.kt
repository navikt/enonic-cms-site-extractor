package no.nav.routing.routes.cms

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.routing.plugins.CmsClientPlugin
import no.nav.routing.plugins.getCmsClientFromCallContext
import no.nav.utils.documentToString
import org.jdom.Document

@Resource("content/{key}")
private class Content(val key: Int)

@Resource("version/{key}")
private class Version(val key: Int)

@Resource("content/query")
private class ContentByQuery(val query: String)

@Resource("menu/{key}")
private class Menu(val key: Int)

@Resource("menuitem/{key}")
private class MenuItem(val key: Int)

@Resource("menudata/{key}")
private class MenuData(val key: Int)

@Resource("categories/{key}")
private class Categories(val key: Int, val depth: Int? = 1)

@Resource("contentByCategory/{key}")
private class ContentByCategory(val key: Int, val depth: Int? = null, val index: Int? = null, val count: Int? = null)

private suspend fun documentXmlResponse(call: ApplicationCall, document: Document) {
    val documentString = documentToString(document);

    if (documentString == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        return call.respondText("Failed to parse XML document")
    }

    call.respondText(documentString, ContentType.Text.Xml)
}

fun Route.cmsClientRoutes() {
    install(CmsClientPlugin)

    install(ContentNegotiation) {
        xml()
    }

    get<Content> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContent(params.key)
        )
    }

    get<Version> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentVersion(params.key)
        )
    }

    get<ContentByQuery> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByQuery(params.query)
        )
    }

    get<Menu> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenu(params.key)
        )
    }

    get<MenuItem> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuItem(params.key)
        )
    }

    get<MenuData> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuData(params.key)
        )
    }

    get<Categories> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getCategories(params.key, params.depth)
        )
    }

    get<ContentByCategory> { params ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByCategory(params.key, params.depth, params.index, params.count)
        )
    }
}