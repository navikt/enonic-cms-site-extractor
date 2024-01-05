package no.nav.cms.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.util.logging.*


private const val ADMIN_PATH = "/admin"
private const val LOGIN_PATH = ADMIN_PATH.plus("/login")
private const val ERROR_PATH = ADMIN_PATH.plus("/errorpage")
private const val ADMIN_PAGE_PATH = ADMIN_PATH.plus("/adminpage")
private const val ADMIN_PREVIEW_PATH = ADMIN_PATH.plus("/preview")
private const val ADMIN_ATTACHMENT_PATH = ADMIN_PATH.plus("/_attachment")

private val logger = KtorSimpleLogger("CmsRestClient")

class CmsRestClient(cmsOrigin: String, private val credential: UserPasswordCredential) {
    private val origin: String = cmsOrigin
    private val loginUrl: String = cmsOrigin.plus(LOGIN_PATH)
    private val errorUrl: String = cmsOrigin.plus(ERROR_PATH)
    private val adminUrl: String = cmsOrigin.plus(ADMIN_PAGE_PATH)
    private val attachmentUrl: String = cmsOrigin.plus(ADMIN_ATTACHMENT_PATH)

    private val loginRedirectUrls: List<String> = listOf(loginUrl, cmsOrigin.plus(":443").plus(ADMIN_PATH))

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpCookies)
        followRedirects = false
    }

    private fun isLoginRedirect(response: HttpResponse): Boolean {
        return response.status == HttpStatusCode.Found
                && response.headers[HttpHeaders.Location] in loginRedirectUrls
    }

    private fun isErrorRedirect(response: HttpResponse): Boolean {
        return response.status == HttpStatusCode.Found
                && response.headers[HttpHeaders.Location] == errorUrl
    }

    private fun isLoginSuccessful(response: HttpResponse): Boolean {
        return !(isErrorRedirect(response) || isLoginRedirect(response))
    }

    private suspend fun login(): HttpResponse {
        val response = httpClient.post(loginUrl) {
            contentType(ContentType.Application.FormUrlEncoded)

            headers {
                append(HttpHeaders.Referrer, loginUrl)
            }

            setBody(FormDataContent(parameters {
                append("username", credential.name)
                append("password", credential.password)
                append("login", "true")
            }))
        }

        return response
    }

    private suspend fun requestWithLogin(
        url: String,
        method: HttpMethod = HttpMethod.Get,
        reqBuilder: HttpRequestBuilder.() -> Unit
    ): HttpResponse {
        val response = httpClient.request(url) {
            this.method = method
            reqBuilder()
        }

        if (!isLoginRedirect(response)) {
            return response
        }

        logger.info("Redirected to login")

        val loginResponse = login()

        return if (isLoginSuccessful(loginResponse)) {
            logger.info("Login was successful!")
            requestWithLogin(url, method) {
                reqBuilder()
            }
        } else {
            logger.error("Login failed!")
            loginResponse
        }
    }

    suspend fun getPageTemplateKey(contentKey: String, versionKey: String, pageKey: String, unitKey: String): String? {
        val response = requestWithLogin(adminUrl) {
            url {
                parameters.append("op", "preview")
                parameters.append("subop", "list")
                parameters.append("page", pageKey)
                parameters.append("selectedunitkey", unitKey)
                parameters.append("contentkey", contentKey)
                parameters.append("versionkey", versionKey)
            }
        }

        if (response.status != HttpStatusCode.OK) {
            return null
        }

        val body = response.bodyAsText()

        return Regex("""pagetemplatekey=(?<pageTemplateKey>\d+)""")
            .find(body)
            ?.groups
            ?.get("pageTemplateKey")
            ?.value
    }

    suspend fun getAttachmentFile(contentKey: Int, binaryKey: Int, versionKey: Int): ByteArray? {
        val response = requestWithLogin(attachmentUrl) {
            url {
                appendPathSegments(listOf(contentKey.toString(), "binary", binaryKey.toString()))
                parameters.append("_version", versionKey.toString())
            }
        }

        return response.body()
    }

    suspend fun renderContent(params: ContentRenderParams): String? {
        val response = requestWithLogin(adminUrl) {
            url {
                parameters.append("op", "preview")
                parameters.append("subop", "pagetemplate")
                parameters.append("contentkey", params.contentkey)
                parameters.append("versionkey", params.versionkey)
                parameters.append("page", params.page)
                parameters.append("selectedunitkey", params.selectedunitkey)
                parameters.append("menukey", params.menukey)
                parameters.append("menuitemkey", params.menuitemkey)
                parameters.append("pagetemplatekey", params.pagetemplatekey)
            }
        }

        if (response.status != HttpStatusCode.OK) {
            return null
        }

        val urlPrefix = this.origin.plus(ADMIN_PREVIEW_PATH)

        return response.bodyAsText().replace(urlPrefix, "")
    }
}
