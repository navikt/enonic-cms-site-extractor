package no.nav.migration

import com.jillesvangurp.ktsearch.DocumentIndexResponse
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import no.nav.cms.client.CmsClient
import no.nav.openSearch.OpenSearchClient
import no.nav.utils.withTimestamp
import java.util.*


private val logger = KtorSimpleLogger("CmsContentMigrationStatus")

@Serializable
enum class ElementType {
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
    val categories: MutableMap<Int, String> = mutableMapOf(),
    val contents: MutableMap<Int, String> = mutableMapOf(),
    val versions: MutableMap<Int, String> = mutableMapOf(),
    val binaries: MutableMap<Int, String> = mutableMapOf(),
)

@Serializable
data class CmsMigrationStatusData(
    val jobId: String,
    val params: ICmsMigrationParams,
    val totalCount: CmsDocumentsCount,
    val migratedCount: CmsDocumentsCount,
    val log: List<String>,
    val results: CmsMigrationResults,
    val remaining: CmsDocumentsKeys? = null,
)

class CmsMigrationStatus(
    private val params: ICmsMigrationParams,
    cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient
) {
    private val jobId: String = UUID.randomUUID().toString()

    private val logEntries: MutableList<String> = mutableListOf()

    private val results = CmsMigrationResults()

    private val documentsRemaining = CmsMigrationDocumentsEnumerator(params, cmsClient).run()

    private val totalCount = CmsDocumentsCount(
        documentsRemaining.categories.size,
        documentsRemaining.contents.size,
        documentsRemaining.versions.size,
        documentsRemaining.binaries.size
    )

    fun log(msg: String, isError: Boolean = false) {
        logEntries.add(withTimestamp(msg))

        if (isError) {
            logger.error(msg)
        } else {
            logger.info(msg)
        }
    }

    fun setResult(key: Int, type: ElementType, msg: String) {
        val resultsMap = when (type) {
            ElementType.CATEGORY -> results.categories
            ElementType.CONTENT -> results.contents
            ElementType.VERSION -> results.versions
            ElementType.BINARY -> results.binaries
        }

        val msgWithTimestamp = withTimestamp(msg)

        if (resultsMap.containsKey(key)) {
            log(
                "Duplicate results for $type $key - Previous result: ${resultsMap[key]} - New result: $msgWithTimestamp",
                true
            )
        } else {
            val documentsRemaining = when (type) {
                ElementType.CATEGORY -> documentsRemaining.categories
                ElementType.CONTENT -> documentsRemaining.contents
                ElementType.VERSION -> documentsRemaining.versions
                ElementType.BINARY -> documentsRemaining.binaries
            }

            documentsRemaining.remove(key)
        }

        resultsMap[key] = msgWithTimestamp
    }

    fun getStatus(withRemaining: Boolean = false): CmsMigrationStatusData {
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
            results = results,
            remaining = if (withRemaining) documentsRemaining else null,
        )
    }

    suspend fun persistToDb(): DocumentIndexResponse {
        return openSearchClient.indexMigrationLog(getStatus(true))
    }
}