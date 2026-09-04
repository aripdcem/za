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
    }
}

rootProject.name = "za"

include(":app")
include(":games:tetris")
include(":games:g2048")
include(":games:snake")
include(":games:sudoku")
include(":games:mines")
include(":games:besharf")
include(":games:kiskac")
include(":games:turetme")
include(":games:dizgi")
include(":games:kuyu")
include(":games:gecit")
include(":games:tavla")
