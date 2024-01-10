package no.nav.migration

import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import no.nav.utils.withTimestamp


private val logger = KtorSimpleLogger("CmsContentMigrationStatus")

@Serializable
enum class CmsMigratorState {
    NOT_STARTED, RUNNING, ABORTED, FAILED, FINISHED
}

@Serializable
enum class ElementType {
    CATEGORY, CONTENT, VERSION, BINARY
}

@Serializable
data class MigratedCount(
    var categories: Int = 0,
    var contents: Int = 0,
    var versions: Int = 0,
    var binaries: Int = 0,
)

@Serializable
data class CmsMigratorStatus(
    private val params: ICmsMigrationParams,
    private val count: CmsDocumentsCount,
    private val remainingElements: CmsDocumentsLists
) {
    private val migratedCount = MigratedCount()

    var state: CmsMigratorState = CmsMigratorState.NOT_STARTED

    private val logEntries: MutableList<String> = mutableListOf()

    private val categoryResults: MutableMap<Int, String> = mutableMapOf()
    private val contentResults: MutableMap<Int, String> = mutableMapOf()
    private val versionResults: MutableMap<Int, String> = mutableMapOf()
    private val binaryResults: MutableMap<Int, String> = mutableMapOf()

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
            ElementType.CATEGORY -> categoryResults
            ElementType.CONTENT -> contentResults
            ElementType.VERSION -> versionResults
            ElementType.BINARY -> binaryResults
        }

        val msgWithTimestamp = withTimestamp(msg)

        if (resultsMap.containsKey(key)) {
            log(
                "Duplicate results for $type $key - Previous result: ${resultsMap[key]} - New result: $msgWithTimestamp",
                true
            )
        } else {
            when (type) {
                ElementType.CATEGORY -> {
                    migratedCount.categories++
                    remainingElements.categories.remove(key)
                }

                ElementType.CONTENT -> {
                    migratedCount.contents++
                    remainingElements.contents.remove(key)
                }

                ElementType.VERSION -> {
                    migratedCount.versions++
                    remainingElements.versions.remove(key)
                }

                ElementType.BINARY -> {
                    migratedCount.binaries++
                    remainingElements.binaries.remove(key)
                }
            }
        }

        resultsMap[key] = msgWithTimestamp
    }
}
