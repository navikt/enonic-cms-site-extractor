package no.nav.extractor

import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient


class CmsCategoryExtractor(
    cmsClient: CmsClient,
    openSearchClient: OpenSearchClient,
) : CmsExtractor(cmsClient, openSearchClient) {
    suspend fun run(
        categoryKey: Int,
        withChildren: Boolean?,
        withContent: Boolean?,
        withVersions: Boolean?
    ) {
        if (start()) {
            extractCategory(
                categoryKey,
                withChildren ?: false,
                withContent ?: false,
                withVersions ?: false
            )

            stop()
        }
    }
}
