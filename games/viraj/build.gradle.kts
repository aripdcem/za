import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Viraj oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Sabit adımlı (60 Hz) simülasyon; tohumdan deterministik pist üretimi.
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
    // Ölçüm koşumları (*Probe) birim testi değildir: rapor üretir, CI'da koşmaz.
    filter { excludeTestsMatching("*Probe") }
}

// Denge ölçümü: ./gradlew :games:viraj:probe  (bkz. docs/oyun-testi.md)
tasks.register<Test>("probe") {
    description = "Denge ölçüm koşumunu çalıştırır ve raporu basar."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnit()
    filter { includeTestsMatching("*Probe") }
    outputs.upToDateWhen { false }
    testLogging { showStandardStreams = true }
}
