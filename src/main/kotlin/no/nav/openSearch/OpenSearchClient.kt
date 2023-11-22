package no.nav.openSearch

import io.ktor.server.auth.*
import io.ktor.util.logging.*
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder


private val logger = KtorSimpleLogger("OpenSearchClient")

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
        val httpHost = HttpHost("https", hostname, port)

        val credentialsProvider = BasicCredentialsProvider()
        credentialsProvider.setCredentials(
            AuthScope(httpHost),
            UsernamePasswordCredentials(
                credentials.name,
                credentials.password.toCharArray()
            )
        )

        val transportBuilder = ApacheHttpClient5TransportBuilder.builder(httpHost)
        transportBuilder.setHttpClientConfigCallback { httpClientBuilder ->
            val tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(
                    SSLContextBuilder
                        .create()
                        .build()
                )
                .build()

            val connectionManager = PoolingAsyncClientConnectionManagerBuilder
                .create()
                .setTlsStrategy(tlsStrategy)
                .build()

            return@setHttpClientConfigCallback httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider)
                .setConnectionManager(connectionManager)
        }

        val transport = transportBuilder.build()

        return OpenSearchClient(transport)
    }
}

