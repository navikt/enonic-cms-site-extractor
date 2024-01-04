package no.nav.cms.client

import com.enonic.cms.api.client.ClientException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.logging.*
import no.nav.utils.getConfigVar


private val logger = KtorSimpleLogger("CmsClientBuilder")

class CmsClientBuilder(environment: ApplicationEnvironment?) {
    private val url = getConfigVar("cms.url", environment)
    private val user = getConfigVar("cms.user", environment)
    private val password = getConfigVar("cms.password", environment)

    fun build(): CmsClient? {
        if (url == null || user == null || password == null) {
            logger.error("CMS service parameters not found!")
            return null
        }

        val client = try {
            CmsClient(url, UserPasswordCredential(user, password))
        } catch (e: ClientException) {
            logger.error("Client exception: ${e.message}")
            null
        }

        if (client == null) {
            logger.error("Failed to initialize CMS client for $user to $url")
        }

        return client
    }
}
