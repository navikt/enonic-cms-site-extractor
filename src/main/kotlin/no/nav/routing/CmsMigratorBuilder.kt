package no.nav.routing

import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.cms.client.CmsClientBuilder
import no.nav.migration.*
import no.nav.migration.status.CmsMigrationStatusBuilder
import no.nav.openSearch.OpenSearchClient
import no.nav.openSearch.OpenSearchClientBuilder


private val logger = KtorSimpleLogger("CmsMigratorBuilder")

private data class Clients(
    val cmsClient: CmsClient,
    val openSearchClient: OpenSearchClient
)

class CmsMigratorBuilder {
    suspend fun build(params: ICmsMigrationParams, environment: ApplicationEnvironment?): CmsMigrator? {
        val clients = buildClients(environment) ?: return null

        val status = CmsMigrationStatusBuilder(clients.cmsClient, clients.openSearchClient)
            .build(params)

        return CmsMigrator(status, clients.cmsClient, clients.openSearchClient)
    }

    suspend fun build(jobId: String, environment: ApplicationEnvironment?): CmsMigrator? {
        val clients = buildClients(environment) ?: return null

        val status = CmsMigrationStatusBuilder(clients.cmsClient, clients.openSearchClient).build(jobId) ?: return null

        return CmsMigrator(status, clients.cmsClient, clients.openSearchClient)
    }

    private suspend fun buildClients(environment: ApplicationEnvironment?): Clients? {
        val cmsClient = CmsClientBuilder(environment).build()
        if (cmsClient == null) {
            logger.error("Failed to initialize CMS client")
            return null
        }

        val openSearchClient = OpenSearchClientBuilder(environment).build()
        if (openSearchClient == null) {
            logger.error("Failed to initialize OpenSearch client")
            return null
        }

        return Clients(cmsClient, openSearchClient)
    }
}