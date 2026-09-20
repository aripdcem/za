import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kelime oyunlarının paylaştığı dil altyapısı: alfabe, sözlük sırası, klavye
// düzeni ve ön-kodlu liste okuyucusu. Saf Kotlin/JVM, Android'e bağımlı değil.
// Listelerin kendisi oyun modüllerinin kaynaklarındadır (bkz. tools/gen_wordlists.py).
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
    filter { excludeTestsMatching("*Probe") }
}
