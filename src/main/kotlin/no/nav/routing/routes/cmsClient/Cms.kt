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

@Resource("/cms")
private class Cms() {
    @Resource("content/{key}")
    class Content(val parent: Cms = Cms(), val key: Int)

    @Resource("version/{key}")
    class Version(val parent: Cms = Cms(), val key: Int)

    @Resource("menu/{key}")
    class Menu(val parent: Cms = Cms(), val key: Int)

    @Resource("menuitem/{key}")
    class MenuItem(val parent: Cms = Cms(), val key: Int)

    @Resource("menudata/{key}")
    class MenuData(val parent: Cms = Cms(), val key: Int)

    @Resource("categories/{key}")
    class Categories(val parent: Cms = Cms(), val key: Int, val depth: Int?)

    @Resource("contentByCategory/{key}")
    class ContentByCategory(val parent: Cms = Cms(), val key: Int)
}

private suspend fun documentXmlResponse(call: ApplicationCall, document: Document) {
    call.respondText(documentToString(document) ?: "Failed to parse XML document", ContentType.Text.Xml)
}

fun Route.cmsClientRoutes() {
    install(CmsClientPlugin)

    get<Cms.Content> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContent(content.key)
        )
    }

    get<Cms.Version> { version ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentVersion(version.key)
        )
    }

    get<Cms.Menu> { menu ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenu(menu.key)
        )
    }

    get<Cms.MenuItem> { menuitem ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuItem(menuitem.key)
        )
    }

    get<Cms.MenuData> { menudata ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuData(menudata.key)
        )
    }

    get<Cms.Categories> { category ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getCategories(category.key, category.depth)
        )
    }

    get<Cms.ContentByCategory> { category ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByCategory(category.key)
        )
    }
}