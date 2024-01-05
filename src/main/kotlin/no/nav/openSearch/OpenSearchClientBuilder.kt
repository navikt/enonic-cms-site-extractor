package no.nav.openSearch

import com.jillesvangurp.ktsearch.KtorRestClient
import com.jillesvangurp.ktsearch.SearchClient
import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.utils.getConfigVar


private val logger = KtorSimpleLogger("OpenSearchClientBuilder")

class OpenSearchClientBuilder(environment: ApplicationEnvironment?) {
    private val host = getConfigVar("opensearch.host", environment)
    private val port = getConfigVar("opensearch.port", environment)?.toInt()
    private val user = getConfigVar("opensearch.user", environment)
    private val password = getConfigVar("opensearch.password", environment)
    private val indexPrefix = getConfigVar("opensearch.indexPrefix", environment)

    suspend fun build(): OpenSearchClient? {
        if (host == null || port == null || user == null || password == null || indexPrefix == null) {
            logger.error("OpenSearch service parameters not found")
            return null
        }

        val searchClient = SearchClient(
            KtorRestClient(
                host = host,
                port = port,
                user = user,
                password = password,
                https = true,
            )
        )

        return OpenSearchClient(searchClient, indexPrefix).initIndices()
    }
}
