package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.JsonObject


private val logger = KtorSimpleLogger("OpenSearchKtClientWrapper")

class OpenSearchClientWrapper(searchClient: SearchClient) {
    private val client: SearchClient

    init {
        this.client = searchClient
    }

    suspend fun info(): SearchEngineInformation {
        return this.client.engineInfo()
    }

    suspend fun createIndexIfNotExist(index: String): Boolean {
        val existsResponse = this.client.exists(index)
        if (existsResponse) {
            logger.info("Index already exists: $index")
            return true
        }

        return this.client.createIndex(index).acknowledged
    }

    suspend fun deleteIndex(index: String) {
        return this.client.deleteIndex(index)
    }

    suspend fun getIndex(index: String): JsonObject {
        return this.client.getIndex(index)
    }


    fun updateIndexMappings(index: String) {

    }
}