package no.nav.routing.cms

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.cms.client.CmsClientBuilder
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder
import no.nav.utils.jsonResponse
import no.nav.utils.xmlResponse


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

@Resource("build")
private class Build {
    @Resource("content/{contentKey}")
    class Content(val parent: Build = Build(), val contentKey: Int)

    @Resource("version/{versionKey}")
    class Version(val parent: Build = Build(), val versionKey: Int)

    @Resource("category/{categoryKey}")
    class Category(val parent: Build = Build(), val categoryKey: Int)
}

fun Route.cmsClientRoutes() {
    install(ContentNegotiation) {
        xml()
        json()
    }

    get<Content.Get> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getContent(it.key)
        )
    }

    get<Content.Query> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getContentByQuery(it.query)
        )
    }

    get<Content.Render> {
        val result = CmsClientBuilder(this@cmsClientRoutes.environment)
            .build()
            ?.renderContent(it.key)
        call.respondText(result ?: "Failed to render content for ${it.key}")
    }

    get<Version.Get> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getContentVersion(it.key)
        )
    }

    get<Version.Render> {
        val result = CmsClientBuilder(this@cmsClientRoutes.environment)
            .build()
            ?.renderVersion(it.key)
        call.respondText(result ?: "Failed to render content version for ${it.key}")
    }

    get<Menu> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getMenu(it.key)
        )
    }

    get<MenuItem> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getMenuItem(it.key)
        )
    }

    get<MenuData> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getMenuData(it.key)
        )
    }

    get<Category> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getCategory(it.key, it.depth)
        )
    }

    get<ContentByCategory> {
        xmlResponse(
            call,
            CmsClientBuilder(this@cmsClientRoutes.environment)
                .build()
                ?.getContentByCategory(it.key, it.depth, it.index, it.count)
        )
    }

    get<Build.Category> {
        val cmsClient = CmsClientBuilder(this@cmsClientRoutes.environment).build()
        if (cmsClient == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            return@get call.respond("Failed to initialize CMS client")
        }

        val document = OpenSearchCategoryDocumentBuilder(cmsClient)
            .build(it.categoryKey)

        jsonResponse(call, document, "Failed to build category document for ${it.categoryKey}")
    }

    get<Build.Content> {
        val cmsClient = CmsClientBuilder(this@cmsClientRoutes.environment).build()
        if (cmsClient == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            return@get call.respond("Failed to initialize CMS client")
        }

        val document = OpenSearchContentDocumentBuilder(cmsClient)
            .buildDocumentFromContent(it.contentKey)

        jsonResponse(call, document, "Failed to build content document for ${it.contentKey}")
    }

    get<Build.Version> {
        val cmsClient = CmsClientBuilder(this@cmsClientRoutes.environment).build()
        if (cmsClient == null) {
            call.response.status(HttpStatusCode.InternalServerError)
            return@get call.respond("Failed to initialize CMS client")
        }

        val document = OpenSearchContentDocumentBuilder(cmsClient)
            .buildDocumentFromVersion(it.versionKey)

        jsonResponse(call, document, "Failed to build content document for version ${it.versionKey}")
    }
}
