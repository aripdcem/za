import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Sürüm tek kaynaktan yönetilir: release.yml, etiketten türettiği sürümü
// -PzaVersion=X.Y.Z olarak geçirir; yerel derlemeler alttaki varsayılanı
// kullanır. versionCode = major*10000 + minor*100 + patch.
val zaVersion: String = (project.findProperty("zaVersion") as? String) ?: "0.12.4"
val zaVersionCode: Int = zaVersion.split('.').map { it.toInt() }.let { (major, minor, patch) ->
    require(major < 214 && minor < 100 && patch < 100) { "Geçersiz sürüm: $zaVersion" }
    // AGP, versionCode için pozitif tamsayı ister.
    (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
}

android {
    namespace = "com.za.games"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.za.games"
        minSdk = 26
        targetSdk = 35
        versionCode = zaVersionCode
        versionName = zaVersion
    }

    // Release imzası CI'da ortam değişkenleriyle sağlanır (bkz. release.yml).
    // Değişkenler yoksa imzasız release üretilir; debug derlemeler etkilenmez.
    val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    if (releaseKeystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "za"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":games:tetris"))
    implementation(project(":games:g2048"))
    implementation(project(":games:snake"))
    implementation(project(":games:sudoku"))
    implementation(project(":games:mines"))
    implementation(project(":games:besharf"))
    implementation(project(":games:kiskac"))
    implementation(project(":games:turetme"))
    implementation(project(":games:dizgi"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
