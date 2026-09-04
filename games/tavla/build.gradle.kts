import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Tavla oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Klasik, Tapa ve Hapis kuralları; deterministik zar; sezgisel bilgisayar rakip.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
