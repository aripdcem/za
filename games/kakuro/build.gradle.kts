import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kakuro oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Tohumdan deterministik üretim; her bulmaca tek çözümlüdür (yayılımlı çözücüyle doğrulanır).
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
