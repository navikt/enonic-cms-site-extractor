package no.nav.migration

import com.jillesvangurp.ktsearch.DocumentIndexResponse
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.utils.withTimestamp
import java.util.*


private val logger = KtorSimpleLogger("CmsMigrationStatus")

@Serializable
enum class CmsElementType {
    CATEGORY, CONTENT, VERSION, BINARY
}

@Serializable
data class CmsDocumentsCount(
    var categories: Int = 0,
    var contents: Int = 0,
    var versions: Int = 0,
    var binaries: Int = 0,
)

@Serializable
data class CmsMigrationResults(
    val categories: List<String>,
    val contents: List<String>,
    val versions: List<String>,
    val binaries: List<String>,
)

@Serializable
data class CmsMigrationStatusData(
    val jobId: String,
    val params: ICmsMigrationParams,
    val totalCount: CmsDocumentsCount,
    val migratedCount: CmsDocumentsCount,
    val log: List<String>,
    val results: CmsMigrationResults? = null,
    val remaining: CmsDocumentsKeys? = null,
)

private data class Results(
    val categories: MutableMap<Int, String> = mutableMapOf(),
    val contents: MutableMap<Int, String> = mutableMapOf(),
    val versions: MutableMap<Int, String> = mutableMapOf(),
    val binaries: MutableMap<Int, String> = mutableMapOf(),
)

class CmsMigrationStatus(
    private val params: ICmsMigrationParams,
    cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient
) {
    private val jobId: String = UUID.randomUUID().toString()

    private val logEntries: MutableList<String> = mutableListOf()

    private val results = Results()

    private val documentsRemaining = CmsMigrationDocumentsEnumerator(params, cmsClient).run()

    private val totalCount = CmsDocumentsCount(
        documentsRemaining.categories.size,
        documentsRemaining.contents.size,
        documentsRemaining.versions.size,
        documentsRemaining.binaries.size
    )

    fun log(msg: String, isError: Boolean = false) {
        val msgWithTimestamp = withTimestamp(msg)

        if (isError) {
            logEntries.add("[Error] $msgWithTimestamp")
            logger.error(msg)
        } else {
            logEntries.add(msgWithTimestamp)
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

        val resultsMap = when (type) {
            CmsElementType.CATEGORY -> results.categories
            CmsElementType.CONTENT -> results.contents
            CmsElementType.VERSION -> results.versions
            CmsElementType.BINARY -> results.binaries
        }

        if (resultsMap.containsKey(key)) {
            log(
                "Duplicate results for $type $key - Previous result: ${resultsMap[key]} - New result: $msg",
                true
            )
        } else {
            val documentsRemaining = when (type) {
                CmsElementType.CATEGORY -> documentsRemaining.categories
                CmsElementType.CONTENT -> documentsRemaining.contents
                CmsElementType.VERSION -> documentsRemaining.versions
                CmsElementType.BINARY -> documentsRemaining.binaries
            }

            documentsRemaining.remove(key)
        }

        resultsMap[key] = msg
    }

    fun getStatus(withResults: Boolean? = false, withRemaining: Boolean? = false): CmsMigrationStatusData {
        val resultsLists = if (withResults == true) {
            CmsMigrationResults(
                results.categories.values.toList(),
                results.contents.values.toList(),
                results.versions.values.toList(),
                results.binaries.values.toList(),
            )
        } else {
            null
        }

        return CmsMigrationStatusData(
            jobId = jobId,
            params = params,
            totalCount = totalCount,
            migratedCount = CmsDocumentsCount(
                results.categories.size,
                results.contents.size,
                results.versions.size,
                results.binaries.size,
            ),
            log = logEntries,
            results = resultsLists,
            remaining = if (withRemaining == true) documentsRemaining else null,
        )
    }

    suspend fun persistToDb() {
        val response = openSearchClient.indexMigrationLog(getStatus(true))
        if (response == null) {
            log("Failed to persist status for job $jobId to OpenSearch!")
        } else {
            log("Response from OpenSearch indexing: $response")
        }
    }
}
