package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.KtorRestClient
import com.jillesvangurp.ktsearch.SearchClient
import io.ktor.server.auth.*


class OpenSearchClientBuilder(
    hostname: String,
    port: Int,
    credentials: UserPasswordCredential,
    https: Boolean = true
) {
    private val hostname: String
    private val port: Int
    private val https: Boolean
    private val credentials: UserPasswordCredential

    init {
        this.hostname = hostname
        this.port = port
        this.https = https
        this.credentials = credentials
    }

    suspend fun build(): OpenSearchClient {
        val searchClient = SearchClient(
            KtorRestClient(
                host = hostname,
                port = port,
                https = https,
                user = credentials.name,
                password = credentials.password
            )
        )

        return OpenSearchClient(searchClient, "cmssbs").initIndices()
    }
}
