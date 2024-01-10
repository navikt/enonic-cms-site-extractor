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
sealed class CmsMigratorStatusBase {
    abstract val key: Int
    private val logEntries: MutableList<String> = mutableListOf()

    fun log(msg: String, isError: Boolean = false) {
        logEntries.add(withTimestamp(msg))

        if (isError) {
            logger.error(msg)
        } else {
            logger.info(msg)
        }
    }
}

@Serializable
data class CmsVersionMigrationStatus(override val key: Int) : CmsMigratorStatusBase()

@Serializable
data class CmsContentMigrationStatus(override val key: Int) : CmsMigratorStatusBase() {
    var numVersions: Int = 0

    val versionsStatus: MutableList<CmsVersionMigrationStatus> = mutableListOf()
}

@Serializable
data class CmsCategoryMigrationStatus(override val key: Int) : CmsMigratorStatusBase() {
    var numCategories: Int = 0
    var numContent: Int = 0

    val categoriesStatus: MutableList<CmsCategoryMigrationStatus> = mutableListOf()
    val contentStatus: MutableList<CmsContentMigrationStatus> = mutableListOf()
}

@Serializable
data class CmsMigratorStatus(val params: ICmsMigrationParams, val count: CmsDocumentsCount) : CmsMigratorStatusBase() {
    override val key = params.key

    var state: CmsMigratorState = CmsMigratorState.NOT_STARTED

    val categoriesStatus: MutableList<CmsCategoryMigrationStatus> = mutableListOf()
    val contentStatus: MutableList<CmsContentMigrationStatus> = mutableListOf()
    val versionsStatus: MutableList<CmsVersionMigrationStatus> = mutableListOf()
}
