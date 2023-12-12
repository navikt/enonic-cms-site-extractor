package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.JsonObject
import no.nav.db.openSearch.documents.*


private val logger = KtorSimpleLogger("OpenSearchClient")

class OpenSearchClient(searchClient: SearchClient) {
    private val client: SearchClient

    init {
        this.client = searchClient
    }

    suspend fun info(): SearchEngineInformation {
        return this.client.engineInfo()
    }

    suspend fun createIndices(index: String): Boolean {
        val existsResponse = this.client.exists(index)
        if (existsResponse) {
            logger.info("Index already exists: $index")
            return true
        }

        val result = this.client.createIndex(index) {
            mappings(dynamicEnabled = false, contentIndexMappings)
        }

        return result.acknowledged
    }

    suspend fun deleteIndex(index: String) {
        return this.client.deleteIndex(index)
    }

    suspend fun getIndex(index: String): JsonObject {
        return this.client.getIndex(index)
    }

    suspend fun indexDocument(index: String, document: OpenSearchContentDocument): DocumentIndexResponse {
        return this.client.indexDocument(index, document)
    }
}
