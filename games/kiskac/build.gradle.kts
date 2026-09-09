import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kıskaç oyun motoru: saf Kotlin/JVM, Android'e bağımlı değil.
// Kelime listeleri dışarıdan verilir (uygulama Beş Harf'in listelerini kullanır).
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
    // Ölçüm koşumu, oyunun arayüzde kullandığı kelime listelerini okur.
    testImplementation(project(":games:besharf"))
}

tasks.test {
    useJUnit()
    // Ölçüm koşumları (*Probe) birim testi değildir: rapor üretir, CI'da koşmaz.
    filter { excludeTestsMatching("*Probe") }
}

// Denge ölçümü: ./gradlew :games:kiskac:probe  (bkz. docs/oyun-testi.md)
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
