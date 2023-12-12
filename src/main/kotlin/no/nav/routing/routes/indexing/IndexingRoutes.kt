import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.db.openSearch.documents.OpenSearchContentDocumentBuilder
import no.nav.routing.plugins.CmsClientPlugin
import no.nav.routing.plugins.OpenSearchClientPlugin
import no.nav.routing.plugins.getCmsClientFromCallContext


@Resource("build")
private class Build {
    @Resource("content/{contentKey}")
    class Content(val parent: Build = Build(), val contentKey: Int)

    @Resource("version/{versionKey}")
    class Version(val parent: Build = Build(), val versionKey: Int)
}

fun Route.indexingRoutes() {
    install(OpenSearchClientPlugin)
    install(CmsClientPlugin)
    install(ContentNegotiation) {
        json()
    }

    get<Build.Content> {
        val client = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(client).buildDocumentFromContent(it.contentKey)

        call.respond(document ?: "oh noes")
    }

    get<Build.Version> {
        val client = getCmsClientFromCallContext(call)
        val document = OpenSearchContentDocumentBuilder(client).buildDocumentFromVersion(it.versionKey)

        call.respond(document ?: "oh noes")
    }
}