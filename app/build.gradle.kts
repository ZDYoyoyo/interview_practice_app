import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// 題庫下載網址；可用 ./gradlew assembleDebug -PbankUrl=https://... 覆寫（例如測試用的分支）
val bankUrl: String = (project.findProperty("bankUrl") as String?)
    ?: "https://raw.githubusercontent.com/zdyoyoyo/interview_practice_app/main/data/questions.json"

android {
    namespace = "io.github.zdyoyoyo.backendquiz"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.zdyoyoyo.backendquiz"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.1"
        buildConfigField("String", "BANK_URL", "\"$bankUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 把 repo 根目錄的 data/ 當成 assets 來源：
    // 打包時 data/questions.json 會變成 App 內建題庫 (assets/questions.json)，
    // 題庫只有一份來源，不需要手動複製。
    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("data"))
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // Robolectric 需要
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Room 會把資料庫 schema 輸出成 JSON，之後寫 migration 和測試 migration 都要用到，這個資料夾要進版控
room {
    schemaDirectory("$projectDir/schemas")
}

// ---- Robolectric 的 Android 系統 jar ----
// Robolectric 預設在「測試執行時」自己從 Maven Central 下載約 150MB 的 android-all jar，
// 不經過 Gradle，沒有快取也沒有重試，CI 上常被限流（HTTP 429）而失敗。
// 改由 Gradle 下載（有快取、會重試），再讓 Robolectric 以離線模式讀取。
// 版本字串對應 Robolectric 4.14.x；需要測試用的 SDK 34 與 targetSdk 35。升級 Robolectric 時要一起更新。
val robolectricRuntime: Configuration by configurations.creating { isTransitive = false }
val robolectricJarsDir = layout.buildDirectory.dir("robolectric-jars")
val copyRobolectricJars by tasks.registering(Copy::class) {
    from(robolectricRuntime)
    into(robolectricJarsDir)
}

tasks.withType<Test>().configureEach {
    dependsOn(copyRobolectricJars)
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricJarsDir.get().asFile.absolutePath)
    // 讓單元測試能讀到 repo 裡真正的題庫檔，用來驗證格式
    systemProperty("bankFile", rootProject.file("data/questions.json").absolutePath)
    // Migration 測試要讀舊版 schema（app/schemas/...）來建立舊版資料庫
    systemProperty("roomSchemaDir", file("schemas/io.github.zdyoyoyo.backendquiz.data.local.AppDatabase").absolutePath)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    robolectricRuntime("org.robolectric:android-all-instrumented:14-robolectric-10818077-i7") // SDK 34：測試用
    robolectricRuntime("org.robolectric:android-all-instrumented:15-robolectric-12650502-i7") // SDK 35：targetSdk
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
}
