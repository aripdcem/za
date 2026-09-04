import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Balkon oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Sabit adımlı (60 Hz) simülasyon; tohumdan deterministik hedef akışı ve rüzgâr.
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
