package no.nav.extractor

import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient


class CmsVersionExtractor(
    cmsClient: CmsClient,
    openSearchClient: OpenSearchClient,
    key: Int
) : CmsExtractor(cmsClient, openSearchClient, key) {
    suspend fun run() {
        if (start()) {
            extractVersion(key)

            stop()
        }
    }
}
