package no.nav.extractor

import io.ktor.server.application.*
import io.ktor.util.logging.*
import no.nav.cms.client.CmsClient
import no.nav.cms.client.CmsClientBuilder
import no.nav.db.openSearch.OpenSearchClient
import no.nav.db.openSearch.OpenSearchClientBuilder


private val logger = KtorSimpleLogger("CmsExtractorFactory")

private val extractorConstructorArgs =
    arrayOf<Class<*>>(CmsClient::class.java, OpenSearchClient::class.java, Int::class.java)

object CmsExtractorFactory {
    private val categoryExtractors = HashMap<Int, CmsCategoryExtractor>()
    private val contentExtractors = HashMap<Int, CmsContentExtractor>()
    private val versionExtractors = HashMap<Int, CmsVersionExtractor>()

    private suspend inline fun <reified ExtractorType : CmsExtractor> createOrRetrieveExtractor(
        key: Int,
        environment: ApplicationEnvironment?,
        extractorMap: HashMap<Int, ExtractorType>,
    ): ExtractorType? {
        val existingExtractor = extractorMap[key]
        if (existingExtractor != null) {
            return existingExtractor
        }

        val cmsClient = CmsClientBuilder(environment).build()
        val openSearchClient = OpenSearchClientBuilder(environment).build()

        if (cmsClient == null || openSearchClient == null) {
            logger.error("Failed to initialize required clients")
            return null
        }

        val extractor = ExtractorType::class.java
            .getConstructor(*extractorConstructorArgs)
            .newInstance(cmsClient, openSearchClient, key)

        extractorMap[key] = extractor

        return extractor
    }

    suspend fun createOrRetrieveCategoryExtractor(
        categoryKey: Int,
        environment: ApplicationEnvironment?,
    ): CmsCategoryExtractor? {
        return createOrRetrieveExtractor<CmsCategoryExtractor>(categoryKey, environment, categoryExtractors)
    }

    suspend fun createOrRetrieveContentExtractor(
        contentKey: Int,
        environment: ApplicationEnvironment?,
    ): CmsContentExtractor? {
        return createOrRetrieveExtractor<CmsContentExtractor>(contentKey, environment, contentExtractors)
    }

    suspend fun createOrRetrieveVersionExtractor(
        versionKey: Int,
        environment: ApplicationEnvironment?,
    ): CmsVersionExtractor? {
        return createOrRetrieveExtractor<CmsVersionExtractor>(versionKey, environment, versionExtractors)
    }
}
