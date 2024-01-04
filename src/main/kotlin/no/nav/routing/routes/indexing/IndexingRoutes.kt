import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder
import no.nav.extractor.CmsExtractorFactory
import no.nav.routing.plugins.CmsClientPlugin
import no.nav.routing.plugins.OpenSearchClientPlugin
import no.nav.routing.plugins.getCmsClientFromCallContext


@Resource("build")
private class Build {
    @Resource("content/{contentKey}")
    class Content(val parent: Build = Build(), val contentKey: Int)

    @Resource("version/{versionKey}")
    class Version(val parent: Build = Build(), val versionKey: Int)

    @Resource("category/{categoryKey}")
    class Category(val parent: Build = Build(), val categoryKey: Int)
}

@Resource("index")
private class IndexDocument {
    @Resource("content/{contentKey}")
    class Content(
        val parent: IndexDocument = IndexDocument(),
        val contentKey: Int,
        val withVersions: Boolean? = false
    )

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
        val document = OpenSearchContentDocumentBuilder(cmsClient)
            .buildDocumentFromContent(it.contentKey)

        call.respond(document ?: "Failed to build content document for ${it.contentKey}")
    }

    get<Build.Version> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(cmsClient)
            .buildDocumentFromVersion(it.versionKey)

        call.respond(document ?: "Failed to build content document for version ${it.versionKey}")
    }

    get<Build.Category> {
        val cmsClient = getCmsClientFromCallContext(call)
        val document = OpenSearchCategoryDocumentBuilder(cmsClient)
            .build(it.categoryKey)

        call.respond(document ?: "Failed to build category document for ${it.categoryKey}")
    }

    get<IndexDocument.Content> {
        val response = CmsExtractorFactory
            .createOrRetrieveContentExtractor(it.contentKey, this@indexingRoutes.environment)
            ?.run(it.withVersions)

        call.respond(response ?: "Oh noes")
    }

    get<IndexDocument.Version> {
        val response = CmsExtractorFactory
            .createOrRetrieveVersionExtractor(it.versionKey, this@indexingRoutes.environment)
            ?.run()

        call.respond(response ?: "Oh noes")
    }

    get<IndexDocument.Category> {
        val response = CmsExtractorFactory
            .createOrRetrieveCategoryExtractor(it.categoryKey, this@indexingRoutes.environment)
            ?.run(
                it.withChildren,
                it.withContent,
                it.withVersions
            )

        call.respond(response ?: "Oh noes")
    }
}