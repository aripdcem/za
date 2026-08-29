import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Beş Harf oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Kelime listeleri modül kaynaklarında taşınır (bkz. tools/gen_words.py).
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
