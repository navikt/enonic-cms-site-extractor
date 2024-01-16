package no.nav.migration.status

import CmsDocumentsCount
import CmsDocumentsKeys
import CmsElementType
import CmsElementsMap
import CmsMigrationStatusData
import CmsMigrationStatusSummary
import com.jillesvangurp.ktsearch.DocumentIndexResponse
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import no.nav.openSearch.OpenSearchClient
import no.nav.utils.getTimestamp
import no.nav.utils.withTimestamp


private val logger = KtorSimpleLogger("CmsMigrationStatus")

private fun <Type> getElementsMapValue(type: CmsElementType, map: CmsElementsMap<Type>): Type {
    return when (type) {
        CmsElementType.CATEGORY -> map.categories
        CmsElementType.CONTENT -> map.contents
        CmsElementType.VERSION -> map.versions
        CmsElementType.BINARY -> map.binaries
    }
}

private fun getMigratedCount(migrated: CmsDocumentsKeys): CmsDocumentsCount {
    return CmsDocumentsCount(
        categories = migrated.categories.size,
        contents = migrated.contents.size,
        versions = migrated.versions.size,
        binaries = migrated.binaries.size
    )
}

class CmsMigrationStatus(
    val data: CmsMigrationStatusData,
    private val openSearchClient: OpenSearchClient
) {
    private var resultCount = 0

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

    suspend fun setResult(key: Int, type: CmsElementType, result: DocumentIndexResponse?, msgSuffix: String = "") {
        resultCount++

        if (resultCount % 1000 == 0) {
            persistToOpenSearch()
        }

        val elementDescription = "$type with key $key".plus(msgSuffix)

        if (result == null) {
            log("Failed to index $elementDescription!", true)
            return
        }

        val msg = withTimestamp("Indexed $elementDescription (index: ${result.index} - result: ${result.result})")

        getElementsMapValue(type, data.migrated).let {
            if (it.contains(key)) {
                log(
                    "Duplicate result for $type $key: $msg",
                    true
                )
            } else {
                it.add(key)
                getElementsMapValue(type, data.results).add(msg)
            }
        }

        getElementsMapValue(type, data.remaining).remove(key)
    }

    fun getStatusSummary(): CmsMigrationStatusSummary {
        return CmsMigrationStatusSummary(
            jobId = data.jobId,
            params = data.params,
            migratedCount = getMigratedCount(data.migrated),
            totalCount = data.totalCount,
            log = data.log,
            startTime = data.startTime,
            stopTime = data.stopTime
        )
    }

    private suspend fun persistToOpenSearch() {
        val response = openSearchClient
            .indexMigrationStatus(data.copy(migratedCount = getMigratedCount(data.migrated)))

        if (response == null) {
            log("Failed to persist status for job ${data.jobId} to OpenSearch - retrying in 5 sec")
            delay(5000L)
            persistToOpenSearch()
        } else {
            log("Response from OpenSearch indexing: $response")
        }
    }

    suspend fun finish() {
        data.stopTime = getTimestamp()
        persistToOpenSearch()
    }
}
