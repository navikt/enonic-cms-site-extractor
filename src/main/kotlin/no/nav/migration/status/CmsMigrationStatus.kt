package no.nav.migration.status

import CmsElementType
import CmsMigrationStatusData
import CmsMigrationStatusSummary
import com.jillesvangurp.ktsearch.DocumentIndexResponse
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import no.nav.openSearch.OpenSearchClient
import no.nav.utils.getTimestamp
import no.nav.utils.withTimestamp


private val logger = KtorSimpleLogger("CmsMigrationStatus")

class CmsMigrationStatus(
    val data: CmsMigrationStatusData,
    private val openSearchClient: OpenSearchClient
) {
    fun log(msg: String, isError: Boolean = false) {
        val msgWithTimestamp = withTimestamp(msg)

        if (isError) {
            data.log.add("[Error] $msgWithTimestamp")
            logger.error(msg)
        } else {
            data.log.add(msgWithTimestamp)
            logger.info(msg)
        }
    }

    fun setResult(key: Int, type: CmsElementType, result: DocumentIndexResponse?, msgSuffix: String = "") {
        val elementDescription = "$type with key $key".plus(msgSuffix)

        if (result == null) {
            log("Failed to index $elementDescription!", true)
            return
        }

        val msg = withTimestamp("Indexed $elementDescription (index: ${result.index} - result: ${result.result})")

        when (type) {
            CmsElementType.CATEGORY -> data.migrated.categories
            CmsElementType.CONTENT -> data.migrated.contents
            CmsElementType.VERSION -> data.migrated.versions
            CmsElementType.BINARY -> data.migrated.binaries
        }.let {
            if (it.contains(key)) {
                log(
                    "Duplicate result for $type $key: $msg",
                    true
                )
            } else {
                it.add(key)
                log(msg)
            }
        }

        when (type) {
            CmsElementType.CATEGORY -> data.remaining.categories
            CmsElementType.CONTENT -> data.remaining.contents
            CmsElementType.VERSION -> data.remaining.versions
            CmsElementType.BINARY -> data.remaining.binaries
        }.remove(key)
    }

    fun getStatusSummary(): CmsMigrationStatusSummary {
        return CmsMigrationStatusSummary(
            jobId = data.jobId,
            params = data.params,
            log = data.log,
            totalCount = data.totalCount,
            migratedCount = data.migratedCount,
            startTime = data.startTime,
            stopTime = data.stopTime
        )
    }

    private suspend fun persistToOpenSearch() {
        val response = openSearchClient.indexMigrationStatus(data)
        if (response == null) {
            log("Failed to persist status for job ${data.jobId} to OpenSearch - retrying in 5 sec")
            delay(5000L)
            persistToOpenSearch()
        } else {
            log("Response from OpenSearch indexing: $response")
        }
    }

    fun start() {
        data.startTime = getTimestamp()
    }

    suspend fun finish() {
        data.stopTime = getTimestamp()
        persistToOpenSearch()
    }
}
