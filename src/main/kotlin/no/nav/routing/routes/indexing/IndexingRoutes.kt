import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocumentBuilder
import no.nav.db.openSearch.documents.content.OpenSearchContentDocumentBuilder
import no.nav.migration.*
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

private suspend fun indexingResponse(
    migratorParams: CmsMigratorParams,
    call: ApplicationCall,
    environment: ApplicationEnvironment?
) {
    val migrator = CmsMigratorFactory
        .createOrRetrieveMigrator(
            migratorParams,
            environment
        )

    if (migrator == null) {
        call.response.status(HttpStatusCode.InternalServerError)
        call.respond("Could not initialize CMS migrator")
        return
    }

    if (migrator.state == CmsMigratorState.RUNNING) {
        call.response.status(HttpStatusCode.Forbidden)
        call.respond("Migrator for ${migrator.params.key} is already running")
        return
    }

    val response = migrator.run()
    call.respond(response)
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

    get<IndexDocument.Category> {
        indexingResponse(
            CmsCategoryMigratorParams(
                key = it.categoryKey,
                withChildren = it.withChildren,
                withContent = it.withContent,
                withVersions = it.withVersions
            ),
            call,
            this@indexingRoutes.environment
        )
    }

    get<IndexDocument.Content> {
        indexingResponse(
            CmsContentMigratorParams(
                key = it.contentKey,
                withVersions = it.withVersions
            ),
            call,
            this@indexingRoutes.environment
        )
    }

    get<IndexDocument.Version> {
        indexingResponse(
            CmsVersionMigratorParams(it.versionKey),
            call,
            this@indexingRoutes.environment
        )
    }
}