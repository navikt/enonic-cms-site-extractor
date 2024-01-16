package no.nav.migration.status

import CmsDocumentsCount
import CmsDocumentsKeys
import CmsMigrationResults
import CmsMigrationStatusData
import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.migration.*
import no.nav.openSearch.OpenSearchClient
import java.util.*


private val logger = KtorSimpleLogger("CmsMigrationStatusBuilder")

class CmsMigrationStatusBuilder(
    private val cmsClient: CmsClient,
    private val openSearchClient: OpenSearchClient
) {

    fun build(params: ICmsMigrationParams): CmsMigrationStatus {
        val documentsEnumerated = CmsMigrationDocumentsEnumerator(params, cmsClient).run()

        val data = CmsMigrationStatusData(
            jobId = UUID.randomUUID().toString(),
            params = params,
            totalCount = CmsDocumentsCount(
                documentsEnumerated.categories.size,
                documentsEnumerated.contents.size,
                documentsEnumerated.versions.size,
                documentsEnumerated.binaries.size
            ),
            migratedCount = CmsDocumentsCount(),
            migrated = CmsDocumentsKeys(),
            remaining = documentsEnumerated,
            log = mutableListOf(),
            results = CmsMigrationResults()
        )

        return CmsMigrationStatus(data, openSearchClient)
    }

    suspend fun build(jobIdToResume: String): CmsMigrationStatus? {
        val persistedData = openSearchClient.getMigrationStatus(jobIdToResume)

        if (persistedData == null) {
            logger.error("No data found for job id $jobIdToResume")
            return null
        }

        return CmsMigrationStatus(persistedData, openSearchClient)
    }
}