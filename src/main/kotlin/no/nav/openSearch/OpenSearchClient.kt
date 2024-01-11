package no.nav.openSearch

import com.jillesvangurp.ktsearch.*
import io.ktor.util.logging.*
import no.nav.migration.CmsMigrationStatusData
import no.nav.openSearch.documents.IndexMappings
import no.nav.openSearch.documents.binary.OpenSearchBinaryDocument
import no.nav.openSearch.documents.binary.binaryIndexMappings
import no.nav.openSearch.documents.category.OpenSearchCategoryDocument
import no.nav.openSearch.documents.category.categoryIndexMappings
import no.nav.openSearch.documents.content.OpenSearchContentDocument
import no.nav.openSearch.documents.content.contentIndexMappings


private val logger = KtorSimpleLogger("OpenSearchClient")

class OpenSearchClient(searchClient: SearchClient, indexPrefix: String) {
    private val client: SearchClient = searchClient

    private val contentIndexName: String = "${indexPrefix}_content"
    private val categoriesIndexName: String = "${indexPrefix}_categories"
    private val binariesIndexName: String = "${indexPrefix}_binaries"
    private val migrationLogsIndexName: String = "${indexPrefix}_migrationlogs"

    suspend fun init(): OpenSearchClient {
        createIndexIfNotExists(contentIndexName, contentIndexMappings)
        createIndexIfNotExists(categoriesIndexName, categoryIndexMappings)
        createIndexIfNotExists(binariesIndexName, binaryIndexMappings)
        createIndexIfNotExists(migrationLogsIndexName)
        return this
    }

    private suspend fun createIndexIfNotExists(index: String, mappings: IndexMappings? = null): Boolean {
        val existsResponse = client.exists(index)

        if (existsResponse) {
            logger.info("Index already exists: $index")
            return true
        }

        val result = client.createIndex(index) {
            if (mappings != null) {
                mappings(dynamicEnabled = false, mappings)
            }
        }

        logger.info("Result for creating index $index: $result")

        return result.acknowledged
    }

    private suspend inline fun <reified DocumentType> indexDocument(
        index: String,
        document: DocumentType,
        id: String
    ): DocumentIndexResponse? {
        return try {
            client.indexDocument(index, document, id)
        } catch (e: RestException) {
            logger.error("Error while indexing document $id to index $index - [${e.status}] ${e.message}")
            return null
        }
    }

    suspend fun indexContent(document: OpenSearchContentDocument): DocumentIndexResponse? {
        return indexDocument(contentIndexName, document, document.versionKey)
    }

    suspend fun indexCategory(document: OpenSearchCategoryDocument): DocumentIndexResponse? {
        return indexDocument(categoriesIndexName, document, document.key)
    }

    suspend fun indexBinary(document: OpenSearchBinaryDocument): DocumentIndexResponse? {
        return indexDocument(binariesIndexName, document, document.binaryKey)
    }

    suspend fun indexMigrationLog(document: CmsMigrationStatusData): DocumentIndexResponse? {
        return indexDocument(migrationLogsIndexName, document, document.jobId)
    }
}
