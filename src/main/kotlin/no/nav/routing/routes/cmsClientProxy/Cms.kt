package no.nav.routing.routes.cmsClientProxy

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import no.nav.routing.CmsClientPlugin
import no.nav.routing.getCmsClientFromCallContext
import no.nav.utils.documentToString

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

fun Route.cmsClientRoutes() {
    install(CmsClientPlugin)

    get<Cms.Content> { content ->
        val contentDocument = getCmsClientFromCallContext(call).getContent(content.key)
        val contentString = documentToString(contentDocument)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }

    get<Cms.Version> { content ->
        val content = getCmsClientFromCallContext(call).getContentVersion(content.key)
        val contentString = documentToString(content)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }

    get<Cms.Menu> { content ->
        val menu = getCmsClientFromCallContext(call).getMenu(content.key)
        val contentString = documentToString(menu)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }

    get<Cms.MenuItem> { content ->
        val menu = getCmsClientFromCallContext(call).getMenuItem(content.key)
        val contentString = documentToString(menu)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }

    get<Cms.Categories> { content ->
        val menu = getCmsClientFromCallContext(call).getCategories(content.key, content.depth)
        val contentString = documentToString(menu)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }

    get<Cms.ContentByCategory> { content ->
        val menu = getCmsClientFromCallContext(call).getContentByCategory(content.key)
        val contentString = documentToString(menu)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }

    get<Cms.MenuData> { content ->
        val menu = getCmsClientFromCallContext(call).getMenuData(content.key)
        val contentString = documentToString(menu)

        call.respondText(contentString ?: "Oh noes", ContentType.Text.Xml)
    }
}