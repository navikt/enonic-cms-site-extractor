package no.nav.db.openSearch

import com.jillesvangurp.ktsearch.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.JsonObject
import no.nav.db.openSearch.index.*


private val logger = KtorSimpleLogger("OpenSearchClient")


class OpenSearchClient(searchClient: SearchClient) {
    private val client: SearchClient

    init {
        this.client = searchClient
    }

    suspend fun info(): SearchEngineInformation {
        return this.client.engineInfo()
    }

    suspend fun createIndexIfNotExist(index: String): Boolean {
        val existsResponse = this.client.exists(index)
        if (existsResponse) {
            logger.info("Index already exists: $index")
            return true
        }

        val result = this.client.createIndex(index) {
            mappings(dynamicEnabled = false) {
                keyword(ContentDocument::type)
                keyword(ContentDocument::paths)

                text(ContentDocument::html)
                text(ContentDocument::xmlAsString)

                number<Int>(ContentDocument::contentKey)
                number<Int>(ContentDocument::versionKey)

                text(ContentDocument::name)
                text(ContentDocument::displayName)

                objField(ContentDocument::owner) {
                    keyword(CmsUser::userstore)
                    text(CmsUser::name)
                    text(CmsUser::displayName)
                    text(CmsUser::email)
                }

                objField(ContentDocument::modifier) {
                    keyword(CmsUser::userstore)
                    text(CmsUser::name)
                    text(CmsUser::displayName)
                    text(CmsUser::email)
                }

                objField(ContentDocument::versions) {
                    number<Int>(VersionReference::key)
                    number<Int>(VersionReference::statusKey)
                    keyword(VersionReference::status)
                    date(VersionReference::timestamp)

                    text(VersionReference::title)
                    text(VersionReference::comment)

                    objField(VersionReference::modifier) {
                        keyword(CmsUser::userstore)
                        text(CmsUser::name)
                        text(CmsUser::displayName)
                        text(CmsUser::email)
                    }
                }

                objField(ContentDocument::locations) {
                    number<Int>(ContentLocation::siteKey)
                    keyword(ContentLocation::type)
                    number<Int>(ContentLocation::menuItemKey)
                    keyword(ContentLocation::menuItemName)
                    keyword(ContentLocation::menuItemPath)
                    text(ContentLocation::menuItemDisplayName)
                    bool(ContentLocation::home)
                }

                objField(ContentDocument::meta) {
                    number<Int>(ContentMetaData::unitKey)
                    number<Int>(ContentMetaData::state)
                    number<Int>(ContentMetaData::status)
                    keyword(ContentMetaData::published)
                    bool(ContentMetaData::current)
                    keyword(ContentMetaData::languageCode)
                    number<Int>(ContentMetaData::languageKey)
                    number<Int>(ContentMetaData::priority)

                    keyword(ContentMetaData::contentType)
                    number<Int>(ContentMetaData::contentTypeKey)

                    date(ContentMetaData::created)
                    date(ContentMetaData::timestamp)
                    date(ContentMetaData::publishFrom)
                    date(ContentMetaData::publishTo)

                    objField(ContentMetaData::category) {
                        number<Int>(ContentCategory::key)
                        keyword(ContentCategory::name)
                    }
                }
            }
        }

        return result.acknowledged
    }

    suspend fun deleteIndex(index: String) {
        return this.client.deleteIndex(index)
    }

    suspend fun getIndex(index: String): JsonObject {
        return this.client.getIndex(index)
    }
}