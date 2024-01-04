package no.nav.extractor

import no.nav.cms.client.CmsClient
import no.nav.db.openSearch.OpenSearchClient


class CmsCategoryExtractor(
    cmsClient: CmsClient,
    openSearchClient: OpenSearchClient,
    key: Int
) : CmsExtractor(cmsClient, openSearchClient, key) {
    suspend fun run(
        withChildren: Boolean?,
        withContent: Boolean?,
        withVersions: Boolean?
    ) {
        if (start()) {
            extractCategory(
                key,
                withChildren ?: false,
                withContent ?: false,
                withVersions ?: false
            )

            stop()
        }
    }
}
