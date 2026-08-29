import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Tetris oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Böylece kurallar cihazsız test edilebilir ve motor başka platformlara taşınabilir.
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
