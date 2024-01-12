package no.nav.routing

import no.nav.routing.migration.migrationRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.coroutines.delay
import no.nav.migration.CmsMigratorHandler
import no.nav.routing.cms.cmsClientRoutes
import no.nav.utils.getConfigVar


private val logger = KtorSimpleLogger("RoutingConfig")

fun Application.configureRouting() {
    authentication {
        basic("auth-basic") {
            realm = "Access to CMS and OpenSearch routes"
            validate { credentials ->
                val user = getConfigVar("auth.user", this@configureRouting.environment)
                val password = getConfigVar("auth.password", this@configureRouting.environment)

                val credentialsAreDefined = user != null && password != null

                return@validate if (credentialsAreDefined && credentials.name == user && credentials.password == password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    logger.error("Auth failed for user ${credentials.name}")
                    null
                }
            }
        }
    }

    routing {
        authenticate("auth-basic") {
            route("/cms") {
                cmsClientRoutes()
            }

            route("/migrate") {
                migrationRoutes()
            }
        }

        get("/internal/isAlive") {
            call.respondText("I am alive!")
        }

        get("/internal/isReady") {
            call.respondText("I am ready!")
        }

        get("/internal/stop") {
            logger.info("Received stop call, preparing to die!")

            CmsMigratorHandler.abortAll()

            delay(5000L)

            call.respond("Kill me!")
        }
    }
}
