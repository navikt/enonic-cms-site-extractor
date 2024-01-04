package no.nav.extractor

import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient


class CmsVersionExtractor(
    cmsClient: CmsClient,
    openSearchClient: OpenSearchClient,
) : CmsExtractor(cmsClient, openSearchClient) {
    suspend fun run(versionKey: Int) {
        if (start()) {
            extractVersion(versionKey)

            stop()
        }
    }
}
