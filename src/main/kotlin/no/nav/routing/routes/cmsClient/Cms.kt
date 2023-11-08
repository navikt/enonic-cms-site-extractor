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
class Cms() {
    @Resource("content/{key}")
    class Content(val parent: Cms = Cms(), val key: Int)

    @Resource("version/{key}")
    class Version(val parent: Cms = Cms(), val key: Int)

    @Resource("menu/{key}")
    class Menu(val parent: Cms = Cms(), val key: Int)

    @Resource("menuitem/{key}")
    class MenuItem(val parent: Cms = Cms(), val key: Int)

    @Resource("categories/{key}")
    class Categories(val parent: Cms = Cms(), val key: Int, val depth: Int?)

    @Resource("contentByCategory/{key}")
    class ContentByCategory(val parent: Cms = Cms(), val key: Int)

    @Resource("menudata/{key}")
    class MenuData(val parent: Cms = Cms(), val key: Int)
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

    get<Cms.Version> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentVersion(content.key)
        )
    }

    get<Cms.Menu> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenu(content.key)
        )
    }

    get<Cms.MenuItem> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuItem(content.key)
        )
    }

    get<Cms.Categories> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getCategories(content.key, content.depth)
        )
    }

    get<Cms.ContentByCategory> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByCategory(content.key)
        )
    }

    get<Cms.MenuData> { content ->
        documentXmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuData(content.key)
        )
    }
}