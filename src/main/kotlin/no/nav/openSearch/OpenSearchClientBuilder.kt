package no.nav.openSearch

import com.jillesvangurp.ktsearch.KtorRestClient
import com.jillesvangurp.ktsearch.SearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.utils.getConfigVar


private val logger = KtorSimpleLogger("OpenSearchClientBuilder")

class OpenSearchClientBuilder(environment: ApplicationEnvironment?) {
    private val uri = getConfigVar("opensearch.uri", environment)
    private val user = getConfigVar("opensearch.user", environment)
    private val password = getConfigVar("opensearch.password", environment)
    private val indexPrefix = getConfigVar("opensearch.indexPrefix", environment)

    suspend fun build(): OpenSearchClient? {
        if (uri == null || user == null || password == null || indexPrefix == null) {
            logger.error("OpenSearch config variables not found")
            return null
        }

        val url = URLBuilder(uri)

        logger.info("Opensearch url: ${url.host}:${url.port} - user: $user")

        val searchClient = SearchClient(
            KtorRestClient(
                host = url.host,
                port = url.port,
                user = user,
                password = password,
                https = url.protocol.name == "https",
            )
        )

        return OpenSearchClient(searchClient, indexPrefix).initIndices()
    }
}
