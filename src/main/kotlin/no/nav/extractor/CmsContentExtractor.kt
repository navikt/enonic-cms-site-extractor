package no.nav.extractor

import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient


class CmsContentExtractor(
    cmsClient: CmsClient,
    openSearchClient: OpenSearchClient,
    key: Int
) : CmsExtractor(cmsClient, openSearchClient, key) {
    suspend fun run(withVersions: Boolean?) {
        if (start()) {
            extractContent(key, withVersions ?: false)

            stop()
        }
    }
}
