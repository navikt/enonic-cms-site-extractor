package no.nav.openSearch

import com.jillesvangurp.ktsearch.*
import io.ktor.util.logging.*
import no.nav.openSearch.documents.IndexMappings
import no.nav.openSearch.documents.category.OpenSearchCategoryDocument
import no.nav.openSearch.documents.category.categoryIndexMappings
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.contentIndexMappings


private val logger = KtorSimpleLogger("OpenSearchClient")

class OpenSearchClient(searchClient: SearchClient, indexPrefix: String) {
    private val client: SearchClient = searchClient

    private val contentIndexName: String = "${indexPrefix}_content"
    private val categoriesIndexName: String = "${indexPrefix}_categories"

    suspend fun initIndices(): OpenSearchClient {
        createIndexIfNotExists(contentIndexName, contentIndexMappings)
        createIndexIfNotExists(categoriesIndexName, categoryIndexMappings)

        return this
    }

    private suspend fun createIndexIfNotExists(index: String, mappings: IndexMappings): Boolean {
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

    suspend fun indexContentDocument(document: OpenSearchContentDocument): DocumentIndexResponse {
        val id = if (document.isCurrentVersion) document.contentKey else document.versionKey
        return this.client.indexDocument(target = contentIndexName, document = document, id = id)
    }

    suspend fun indexCategoryDocument(document: OpenSearchCategoryDocument): DocumentIndexResponse {
        return this.client.indexDocument(target = categoriesIndexName, document = document, id = document.key)
    }
}
