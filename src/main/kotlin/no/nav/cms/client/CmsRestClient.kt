package no.nav.cms.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.logging.*
import no.nav.cms.renderer.ContentRenderParams


private const val LOGIN_PATH = "/admin/login"
private const val ADMIN_PAGE_PATH = "/admin/adminpage"

private val logger = KtorSimpleLogger("CmsRestClient")

class CmsRestClient(cmsOrigin: String, username: String, password: String) {
    private val origin: String
    private val username: String
    private val password: String

    private val httpClient: HttpClient

    init {
        this.origin = cmsOrigin
        this.username = username
        this.password = password

        this.httpClient = HttpClient(CIO) {
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpCookies)

            followRedirects = false
        }
    }

    private fun getLoginUrl(): String {
        return "${this.origin}$LOGIN_PATH"
    }

    private fun getAdminPageUrl(): String {
        return "${this.origin}$ADMIN_PAGE_PATH"
    }

    private fun isRedirectToLogin(response: HttpResponse): Boolean {
        return response.status == HttpStatusCode.Found && response.headers[HttpHeaders.Location] == this.getLoginUrl()
    }

    private suspend fun login(): Boolean {
        val response = this.httpClient.get(this.getLoginUrl()) {
            url {
                parameters.append("username", this@CmsRestClient.username)
                parameters.append("password", this@CmsRestClient.password)
                parameters.append("login", "true")
            }
        }

        return !isRedirectToLogin(response)
    }

    suspend fun getPageTemplateKey(contentKey: String, versionKey: String, pageKey: String, unitKey: String): String? {
        this.login()

        val response = this.httpClient.get(getAdminPageUrl()) {
            url {
                parameters.append("op", "preview")
                parameters.append("subop", "list")
                parameters.append("page", pageKey)
                parameters.append("selectedunitkey", unitKey)
                parameters.append("contentkey", contentKey)
                parameters.append("versionkey", versionKey)
            }
        }

        if (response.status.value != 200) {
            return null
        }

        val body = response.bodyAsText()

        val pageTemplateKey =
            Regex("""pagetemplatekey=(?<pageTemplateKey>\d+)""")
                .find(body)?.groups
                ?.get("pageTemplateKey")?.value

        return pageTemplateKey
    }

    suspend fun renderPage(params: ContentRenderParams): String? {
        this.login()

        val response = this.httpClient.get(getAdminPageUrl()) {
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

        if (response.status.value != 200) {
            return null
        }

        return response.bodyAsText()
    }
}
