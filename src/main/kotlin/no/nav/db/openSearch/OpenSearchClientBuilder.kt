package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.KtorRestClient
import com.jillesvangurp.ktsearch.SearchClient
import io.ktor.server.auth.*


class OpenSearchClientBuilder(
    hostname: String,
    port: Int,
    credentials: UserPasswordCredential,
    scheme: String = "https"
) {
    private val hostname: String
    private val port: Int
    private val scheme: String
    private val credentials: UserPasswordCredential

    init {
        this.hostname = hostname
        this.port = port
        this.scheme = scheme
        this.credentials = credentials
    }

    fun build(): OpenSearchClient {
        val searchClient = SearchClient(
            KtorRestClient(
                host = hostname,
                port = port,
                https = true,
                user = credentials.name,
                password = credentials.password
            )
        )

        return OpenSearchClient(searchClient)
    }
}

