package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.RestException
import com.jillesvangurp.ktsearch.SearchClient
import com.jillesvangurp.ktsearch.SearchEngineInformation
import com.jillesvangurp.ktsearch.getIndex
import io.ktor.util.logging.*
import kotlinx.serialization.json.JsonObject


private val logger = KtorSimpleLogger("OpenSearchKtClientWrapper")

class OpenSearchKtClientWrapper(searchClient: SearchClient) {
    private val client: SearchClient

    init {
        this.client = searchClient
    }

    suspend fun info(): SearchEngineInformation {
        return this.client.engineInfo()
    }

//    fun createIndexIfNotExist(index: String): Boolean {
//        val indicesClient = this.client.indices()
//
//        val existsRequest = ExistsRequest.Builder().index(index).build()
//
//        val existsResponse = indicesClient.exists(existsRequest)
//        if (existsResponse.value()) {
//            logger.info("Index already exists: $index")
//            return true
//        }
//
//        val createIndexRequest = CreateIndexRequest.Builder().index(index).build()
//
//        return try {
//            val createIndexResponse = indicesClient.create(createIndexRequest)
//            createIndexResponse.acknowledged() ?: false
//        } catch (ex: OpenSearchException) {
//            logger.error("Failed to create index $index - ${ex.message}")
//            false
//        }
//    }
//
//    fun deleteIndex(index: String): Boolean {
//        val deleteRequest = DeleteIndexRequest.Builder().index(index).build()
//
//        return try {
//            val deleteResponse = this.client.indices().delete(deleteRequest)
//            deleteResponse.acknowledged()
//        } catch (ex: OpenSearchException) {
//            logger.error("Failed to delete index $index - ${ex.message}")
//            false
//        }
//    }

    suspend fun getIndex(index: String): JsonObject {
        return this.client.getIndex(index)
    }


    fun updateIndexMappings(index: String) {

    }
}