import kotlinx.serialization.Serializable
import no.nav.migration.ICmsMigrationParams

@Serializable
enum class CmsElementType(val value: String) {
    CATEGORY("category"),
    CONTENT("content"),
    VERSION("version"),
    BINARY("binary")
}

sealed interface CmsElementsMap<Value> {
    val categories: Value
    val contents: Value
    val versions: Value
    val binaries: Value
}

@Serializable
data class CmsDocumentsCount(
    override val categories: Int = 0,
    override val contents: Int = 0,
    override val versions: Int = 0,
    override val binaries: Int = 0,
) : CmsElementsMap<Int>

@Serializable
data class CmsDocumentsKeys(
    override val categories: MutableSet<Int> = mutableSetOf(),
    override val contents: MutableSet<Int> = mutableSetOf(),
    override val versions: MutableSet<Int> = mutableSetOf(),
    override val binaries: MutableSet<Int> = mutableSetOf()
) : CmsElementsMap<MutableSet<Int>>

@Serializable
data class CmsMigrationResults(
    override val categories: MutableList<String> = mutableListOf(),
    override val contents: MutableList<String> = mutableListOf(),
    override val versions: MutableList<String> = mutableListOf(),
    override val binaries: MutableList<String> = mutableListOf(),
) : CmsElementsMap<MutableList<String>>

@Serializable
data class CmsMigrationStatusData(
    val jobId: String,
    val params: ICmsMigrationParams,
    val migratedCount: CmsDocumentsCount,
    val totalCount: CmsDocumentsCount,
    val log: MutableList<String>,
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
    val migratedCount: CmsDocumentsCount,
    val totalCount: CmsDocumentsCount,
    val log: MutableList<String>,
    val startTime: String? = null,
    val stopTime: String? = null,
)
