package no.nav.cms.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.util.logging.*
import no.nav.cms.renderer.ContentRenderParams


private const val LOGIN_PATH = "/admin/login"
private const val ERROR_PATH = "/admin/errorpage"
private const val ADMIN_PAGE_PATH = "/admin/adminpage"

private val logger = KtorSimpleLogger("CmsRestClient")

class CmsRestClient(cmsOrigin: String, credential: UserPasswordCredential) {
    private val origin: String
    private val loginUrl: String
    private val errorUrl: String
    private val adminUrl: String

    private val credential: UserPasswordCredential
    private val httpClient: HttpClient

    init {
        this.origin = cmsOrigin
        this.loginUrl = cmsOrigin.plus(LOGIN_PATH)
        this.errorUrl = cmsOrigin.plus(ERROR_PATH)
        this.adminUrl = cmsOrigin.plus(ADMIN_PAGE_PATH)

        this.credential = credential

        this.httpClient = HttpClient(CIO) {
            install(Logging) {
                level = LogLevel.INFO
            }

            install(HttpCookies)

            followRedirects = false
        }
    }

    private fun isLoginRedirect(response: HttpResponse): Boolean {
        return response.status == HttpStatusCode.Found
                && response.headers[HttpHeaders.Location] == loginUrl
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
        block: HttpRequestBuilder.() -> Unit
    ): HttpResponse {
        val response = httpClient.request(url) {
            this.method = method
            block()
        }

        if (isLoginRedirect(response)) {
            logger.info("Redirected to login")

            val loginResponse = login()

            return if (isLoginSuccessful(loginResponse)) {
                logger.info("Login was successful!")
                requestWithLogin(url, method) {
                    block()
                }
            } else {
                logger.info("Login failed!")
                loginResponse
            }
        }

        return response
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

        val pageTemplateKey =
            Regex("""pagetemplatekey=(?<pageTemplateKey>\d+)""")
                .find(body)?.groups
                ?.get("pageTemplateKey")?.value

        return pageTemplateKey
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

        return response.bodyAsText()
    }
}
