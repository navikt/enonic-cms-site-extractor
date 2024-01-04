import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder
import no.nav.extractor.CmsContentExtractor
import no.nav.routing.plugins.CmsClientPlugin
import no.nav.routing.plugins.OpenSearchClientPlugin
import no.nav.routing.plugins.getCmsClientFromCallContext
import no.nav.routing.plugins.getOpenSearchClientFromCallContext


@Resource("build")
private class Build {
    @Resource("content/{contentKey}")
    class Content(val parent: Build = Build(), val contentKey: Int)

    @Resource("version/{versionKey}")
    class Version(val parent: Build = Build(), val versionKey: Int)

    @Resource("category/{categoryKey}")
    class Category(val parent: Build = Build(), val categoryKey: Int)
}

@Resource("index-document")
private class IndexDocument {
    @Resource("content/{contentKey}")
    class Content(val parent: IndexDocument = IndexDocument(), val contentKey: Int)

    @Resource("version/versionKey}")
    class Version(val parent: IndexDocument = IndexDocument(), val versionKey: Int)

    @Resource("category/{categoryKey}")
    class Category(
        val parent: IndexDocument = IndexDocument(),
        val categoryKey: Int,
        val withChildren: Boolean? = false,
        val withContent: Boolean? = false,
        val withVersions: Boolean? = false
    )
}

fun Route.indexingRoutes() {
    install(OpenSearchClientPlugin)
    install(CmsClientPlugin)
    install(ContentNegotiation) {
        json()
    }

    get<Build.Content> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(it.contentKey)

        call.respond(document ?: "oh noes")
    }

    get<Build.Version> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(it.versionKey)

        call.respond(document ?: "oh noes")
    }

    get<Build.Category> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchCategoryDocumentBuilder(cmsClient).build(it.categoryKey)

        call.respond(document ?: "oh noes")
    }

    get<IndexDocument.Content> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromContent(it.contentKey)

        if (document == null) {
            call.respondText("Failed to build document!")
            return@get
        }

        val response = getOpenSearchClientFromCallContext(call).indexContentDocument(document)

        call.respond(response)
    }

    get<IndexDocument.Version> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(cmsClient).buildDocumentFromVersion(it.versionKey)

        if (document == null) {
            call.respondText("Failed to build document!")
            return@get
        }

        val response = getOpenSearchClientFromCallContext(call).indexContentDocument(document)

        call.respond(response)
    }

    get<IndexDocument.Category> {
        val cmsClient = getCmsClientFromCallContext(call)
        val openSearchClient = getOpenSearchClientFromCallContext(call)

        val response = CmsContentExtractor(cmsClient, openSearchClient).extractCategoryToOpenSearch(
            it.categoryKey,
            it.withChildren,
            it.withContent,
            it.withVersions
        )

        call.respond(response)
    }
}