import kotlinx.serialization.Serializable
import no.nav.migration.ICmsMigrationParams

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
data class CmsDocumentsKeys(
    val categories: MutableSet<Int> = mutableSetOf(),
    val contents: MutableSet<Int> = mutableSetOf(),
    val versions: MutableSet<Int> = mutableSetOf(),
    val binaries: MutableSet<Int> = mutableSetOf()
)

@Serializable
data class CmsMigrationResults(
    val categories: MutableList<String> = mutableListOf(),
    val contents: MutableList<String> = mutableListOf(),
    val versions: MutableList<String> = mutableListOf(),
    val binaries: MutableList<String> = mutableListOf(),
)

@Serializable
data class CmsMigrationStatusData(
    val jobId: String,
    val params: ICmsMigrationParams,
    val log: MutableList<String>,
    val totalCount: CmsDocumentsCount,
    val migratedCount: CmsDocumentsCount,
    val results: CmsMigrationResults,
    val migrated: CmsDocumentsKeys,
    val remaining: CmsDocumentsKeys,
    var startTime: String? = null,
    var stopTime: String? = null,
)

@Serializable
data class CmsMigrationStatusSummary(
    val jobId: String,
    val params: ICmsMigrationParams,
    val log: MutableList<String>,
    val totalCount: CmsDocumentsCount,
    val migratedCount: CmsDocumentsCount,
    val startTime: String? = null,
    val stopTime: String? = null,
)
