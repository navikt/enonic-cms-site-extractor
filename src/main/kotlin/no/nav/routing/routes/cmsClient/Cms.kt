package no.nav.routing.routes.cmsClient

import io.ktor.http.*
import io.ktor.http.cio.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.routing.CmsClientPlugin
import no.nav.routing.getCmsClientFromCallContext
import no.nav.utils.documentToString
import org.jdom.Document

@Resource("content/{key}")
private class Content(val key: Int)

@Resource("version/{key}")
private class Version(val key: Int)

@Resource("menu/{key}")
private class Menu(val key: Int)

@Resource("menuitem/{key}")
private class MenuItem(val key: Int)

@Resource("menudata/{key}")
private class MenuData(val key: Int)

@Resource("categories/{key}")
private class Categories(val key: Int, val depth: Int?)

@Resource("contentByCategory/{key}")
private class ContentByCategory(val key: Int)

private suspend fun documentXmlResponse(call: ApplicationCall, document: Document) {
    call.respondText(documentToString(document) ?: "Failed to parse XML document", ContentType.Text.Xml)
}

fun Route.cmsClientRoutes() {
    install(CmsClientPlugin)

    get<Content> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContent(content.key)
        )
    }

    get<Version> { version ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentVersion(version.key)
        )
    }

    get<Menu> { menu ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenu(menu.key)
        )
    }

    get<MenuItem> { menuitem ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuItem(menuitem.key)
        )
    }

    get<MenuData> { menudata ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuData(menudata.key)
        )
    }

    get<Categories> { category ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getCategories(category.key, category.depth)
        )
    }

    get<ContentByCategory> { category ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByCategory(category.key)
        )
    }
}