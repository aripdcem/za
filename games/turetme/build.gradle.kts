import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kelime Türetme oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Kelime listeleri modül kaynaklarında taşınır (bkz. tools/gen_turetme.py).
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

// Denge ölçümü: ./gradlew :games:turetme:probe  (bkz. docs/oyun-testi.md)
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
