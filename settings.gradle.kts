rootProject.name = "enonic-cms-site-extractor"

pluginManagement {
    val kotlinVersion: String by settings
    val ktorVersion: String by settings

    plugins {
        kotlin("jvm") version (kotlinVersion)
        kotlin("plugin.serialization") version(kotlinVersion)
        id("io.ktor.plugin") version (ktorVersion)
    }
}