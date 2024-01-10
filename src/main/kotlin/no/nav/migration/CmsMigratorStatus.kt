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
data class ElementResult(
    val key: Int,
    val result: String,
)

@Serializable
data class CmsMigratorStatus(private val params: ICmsMigrationParams, private val count: CmsDocumentsCount) {
    private val logEntries: MutableList<String> = mutableListOf()

    var state: CmsMigratorState = CmsMigratorState.NOT_STARTED

    var categoriesMigrated: Int = 0
    var contentsMigrated: Int = 0
    var versionsMigrated: Int = 0
    var binariesMigrated: Int = 0

    val categoryResults: MutableMap<Int, ElementResult> = mutableMapOf()
    val contentResults: MutableMap<Int, ElementResult> = mutableMapOf()
    val versionResults: MutableMap<Int, ElementResult> = mutableMapOf()
    val binaryResults: MutableMap<Int, ElementResult> = mutableMapOf()

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

        resultsMap[key] = ElementResult(key, withTimestamp(msg))
    }
}
