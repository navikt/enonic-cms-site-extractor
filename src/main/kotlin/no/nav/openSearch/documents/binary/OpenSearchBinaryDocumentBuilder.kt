package no.nav.openSearch.documents.binary

import no.nav.cms.client.CmsClient
import no.nav.openSearch.documents.content.ContentBinaryReference


class OpenSearchBinaryDocumentBuilder(private val cmsClient: CmsClient) {

    suspend fun build(
        binaryReference: ContentBinaryReference,
        contentKey: Int,
        versionKey: Int
    ): OpenSearchBinaryDocument? {
        val data = cmsClient.getBinaryDataAsBase64(
            binaryKey = binaryReference.key.toInt(),
            contentKey = contentKey,
            versionKey = versionKey
        )

        if (data == null) {
            return null
        }

        return OpenSearchBinaryDocument(
            binaryKey = binaryReference.key,
            contentKey = contentKey.toString(),
            versionKey = versionKey.toString(),

            filename = binaryReference.filename,
            filesize = binaryReference.filesize,

            data = data
        )
    }
}