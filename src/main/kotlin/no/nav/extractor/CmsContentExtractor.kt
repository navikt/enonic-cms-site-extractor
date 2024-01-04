package no.nav.extractor

import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient


class CmsContentExtractor(
    cmsClient: CmsClient,
    openSearchClient: OpenSearchClient,
) : CmsExtractor(cmsClient, openSearchClient) {
    suspend fun run(
        categoryKey: Int,
        withVersions: Boolean?
    ) {
        if (start()) {
            extractContent(categoryKey, withVersions ?: false)

            stop()
        }
    }
}
