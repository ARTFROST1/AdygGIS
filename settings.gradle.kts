pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Additional Google Maven repository for compatibility
        maven { url = uri("https://maven.google.com/") }
        // Yandex MapKit repository
        maven { url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven/") }
    }
}

rootProject.name = "AdygGIS"
include(":app")
