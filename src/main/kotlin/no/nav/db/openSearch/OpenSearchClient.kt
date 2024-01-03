package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.JsonObject
import no.nav.db.openSearch.documents.IndexMappings
import no.nav.db.openSearch.documents.category.OpenSearchCategoryDocument
import no.nav.db.openSearch.documents.content.OpenSearchContentDocument


private val logger = KtorSimpleLogger("OpenSearchClient")

class OpenSearchClient(searchClient: SearchClient) {
    private val client: SearchClient = searchClient

    suspend fun info(): SearchEngineInformation {
        return this.client.engineInfo()
    }

    suspend fun createIndexIfNotExists(index: String, mappings: IndexMappings): Boolean {
        val existsResponse = this.client.exists(index)
        if (existsResponse) {
            logger.info("Index already exists: $index")
            return true
        }

        val result = this.client.createIndex(index) {
            mappings(dynamicEnabled = false, mappings)
        }

        return result.acknowledged
    }

    suspend fun deleteIndex(index: String) {
        return this.client.deleteIndex(index)
    }

    suspend fun getIndex(index: String): JsonObject {
        return this.client.getIndex(index)
    }

    suspend fun indexContentDocument(index: String, document: OpenSearchContentDocument, id: String): DocumentIndexResponse {
        return this.client.indexDocument(target = index, document = document, id = id)
    }

    suspend fun indexCategoryDocument(index: String, document: OpenSearchCategoryDocument, id: String): DocumentIndexResponse {
        return this.client.indexDocument(target = index, document = document, id = id)
    }
}
