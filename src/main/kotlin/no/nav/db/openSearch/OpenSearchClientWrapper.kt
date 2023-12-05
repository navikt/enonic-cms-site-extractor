package no.nav.db.openSearch

import io.ktor.util.logging.*
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.DeleteIndexRequest
import org.opensearch.client.opensearch.indices.ExistsRequest


private val logger = KtorSimpleLogger("OpenSearchClientWrapper")

class OpenSearchClientWrapper(openSearchClient: OpenSearchClient) {
    private val client: OpenSearchClient

    init {
        this.client = openSearchClient
    }

    fun createIndexIfNotExist(index: String): Boolean {
        val indicesClient = this.client.indices()

        val existsRequest = ExistsRequest.Builder().index(index).build()

        val existsResponse = indicesClient.exists(existsRequest)
        if (existsResponse.value()) {
            logger.info("Index already exists: $index")
            return true
        }

        val createIndexRequest = CreateIndexRequest.Builder().index(index).build()

        return try {
            val createIndexResponse = indicesClient.create(createIndexRequest)
            createIndexResponse.acknowledged() ?: false
        } catch (ex: OpenSearchException) {
            logger.error("Failed to create index $index - ${ex.message}")
            false
        }
    }

    fun deleteIndex(index: String): Boolean {
        val deleteRequest = DeleteIndexRequest.Builder().index(index).build()

        return try {
            val deleteResponse = this.client.indices().delete(deleteRequest)
            deleteResponse.acknowledged()
        } catch (ex: OpenSearchException) {
            logger.error("Failed to delete index $index - ${ex.message}")
            false
        }
    }

    fun ping(): Boolean {
        return this.client.ping().value()
    }

    fun updateIndexMappings(index: String) {

    }
}