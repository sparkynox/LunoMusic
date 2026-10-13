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
        maven("https://raw.githubusercontent.com/bravepipeproject/maven-repo/master/repository")
        maven("https://jitpack.io")
    }
}
rootProject.name = "LunoMusic"
include(":app")
