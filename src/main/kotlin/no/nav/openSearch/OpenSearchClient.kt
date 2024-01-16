package no.nav.openSearch

import CmsMigrationStatusData
import com.jillesvangurp.ktsearch.*
import com.jillesvangurp.searchdsls.querydsl.Script
import io.ktor.util.logging.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
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
    private val categoriesIndex: String = "${indexPrefix}_categories"
    private val binariesIndex: String = "${indexPrefix}_binaries"
    private val migrationStatusIndex: String = "${indexPrefix}_migrationlogs"

    suspend fun init(): OpenSearchClient {
        createIndexIfNotExists(contentIndexName, contentIndexMappings)
        createIndexIfNotExists(categoriesIndex, categoryIndexMappings)
        createIndexIfNotExists(binariesIndex, binaryIndexMappings)
        createIndexIfNotExists(migrationStatusIndex)
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
        return indexDocument(categoriesIndex, document, document.key)
    }

    suspend fun indexBinary(document: OpenSearchBinaryDocument): DocumentIndexResponse? {
        return indexDocument(binariesIndex, document, document.binaryKey)
    }

    suspend fun addVersionKeyToBinaryDocument(binaryKey: Int, versionKey: Int): DocumentUpdateResponse? {
        try {
            return client.updateDocument(binariesIndex, binaryKey.toString(), Script.create {
                source = "if (!ctx._source.versionKeys.contains(params.key)) {ctx._source.versionKeys.add(params.key)}"
                params = mapOf("key" to versionKey.toString())
            })
        } catch (e: RestException) {
            logger.error("Error updating binary document for $binaryKey: [${e.status}] ${e.message}")
            return null
        }
    }

    suspend fun indexMigrationStatus(document: CmsMigrationStatusData): DocumentIndexResponse? {
        return indexDocument(migrationStatusIndex, document, document.jobId)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getMigrationStatus(jobId: String): CmsMigrationStatusData? {
        try {
            val response = client.getDocument(target = migrationStatusIndex, id = jobId)
            return response.document<CmsMigrationStatusData>()
        } catch (e: Exception) {
            when (e) {
                is RestException -> {
                    logger.error("Error getting status for job $jobId: ${e.status} ${e.message}")
                }

                is MissingFieldException -> {
                    logger.error("Missing fields in status for job $jobId: ${e.missingFields.joinToString(", ")} (${e.message})")
                }

                else -> {
                    logger.error("Unknown error getting status for job $jobId - ${e.message}")
                }
            }

            return null
        }
    }
}
