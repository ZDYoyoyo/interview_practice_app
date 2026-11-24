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
        // Robolectric 的 Android 系統 jar 很大（約 150MB），Maven Central 常對 CI 限流（HTTP 429）。
        // 這個 group 優先從 Google 維護的 Maven Central 鏡像下載，找不到才回到 mavenCentral()。
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            content { includeGroup("org.robolectric") }
        }
        mavenCentral()
    }
}

rootProject.name = "BackendQuiz"
include(":app")
