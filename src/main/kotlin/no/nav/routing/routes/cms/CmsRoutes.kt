package no.nav.routing.routes.cms

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.routing.plugins.CmsClientPlugin
import no.nav.routing.plugins.getCmsClientFromCallContext
import no.nav.utils.xmlToString
import org.jdom.Parent

@Resource("content")
private class Content() {
    @Resource("{key}")
    class Get(val parent: Content = Content(), val key: Int)

    @Resource("query")
    class Query(val parent: Content = Content(), val query: String)

    @Resource("render/{key}")
    class Render(val parent: Content = Content(), val key: Int)
}

@Resource("version")
private class Version() {
    @Resource("{key}")
    class Get(val parent: Version = Version(), val key: Int)

    @Resource("render/{key}")
    class Render(val parent: Version = Version(), val key: Int)
}

@Resource("menu/{key}")
private class Menu(val key: Int)

@Resource("menuitem/{key}")
private class MenuItem(val key: Int)

@Resource("menudata/{key}")
private class MenuData(val key: Int)

@Resource("category/{key}")
private class Category(val key: Int, val depth: Int? = 1)

@Resource("contentByCategory/{key}")
private class ContentByCategory(val key: Int, val depth: Int? = null, val index: Int? = null, val count: Int? = null)

private suspend fun <T : Parent> xmlResponse(call: ApplicationCall, xml: T?) {
    if (xml == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        return call.respondText("No XML provided")
    }

    val documentString = xmlToString(xml)

    if (documentString == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        return call.respondText("Failed to parse XML")
    }

    call.respondText(documentString, ContentType.Text.Xml)
}

fun Route.cmsClientRoutes() {
    install(CmsClientPlugin)
    install(ContentNegotiation) {
        xml()
    }

    get<Content.Get> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContent(it.key)
        )
    }

    get<Content.Query> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByQuery(it.query)
        )
    }

    get<Content.Render> {
        val result = getCmsClientFromCallContext(call).renderContent(it.key)
        call.respondText(result ?: "Failed to render content for ${it.key}")
    }

    get<Version.Get> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentVersion(it.key)
        )
    }

    get<Version.Render> {
        val result = getCmsClientFromCallContext(call).renderVersion(it.key)
        call.respondText(result ?: "Failed to render content version for ${it.key}")
    }

    get<Menu> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenu(it.key)
        )
    }

    get<MenuItem> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuItem(it.key)
        )
    }

    get<MenuData> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getMenuData(it.key)
        )
    }

    get<Category> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getCategory(it.key, it.depth)
        )
    }

    get<ContentByCategory> {
        xmlResponse(
            call,
            getCmsClientFromCallContext(call)
                .getContentByCategory(it.key, it.depth, it.index, it.count)
        )
    }
}
