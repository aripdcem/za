import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Sayı oyunları motoru (Vergici, Toplam Kapma): saf Kotlin/JVM, Android'e bağımlı değil.
// Sıra tabanlı; Vergici için tam arama çözücü, Toplam Kapma için minimax.
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
