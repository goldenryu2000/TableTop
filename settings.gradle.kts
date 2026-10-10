pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tabletop"
include(":app")
include(":core:cards-model", ":games:klondike:engine")
include(":core:feedback")
include(":core:game-api")
include(":core:design", ":core:cards-ui", ":core:solitaire-ui")
include(":games:klondike:ui")
include(":games:mancala:engine", ":games:mancala:ui")
include(":games:spider:engine", ":games:spider:ui")
