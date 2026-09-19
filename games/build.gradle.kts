// games/ altındaki bütün motor modüllerinin testleri tek görevde. Liste elle
// tutulmaz: alt projeler settings.gradle.kts'ten türetilir, böylece yeni bir
// motor eklenince CI ve sürüm iş akışı onu kendiliğinden koşar.
//
// Görev bilinçli olarak kökte değil burada: kök görev istendiğinde Gradle :app'i
// de yapılandırır ve Android SDK'sı olmayan ortamda derleme çöker. Böyle
// çalışır: ./gradlew --configure-on-demand :games:engineTests
tasks.register("engineTests") {
    group = "verification"
    description = "games/ altındaki bütün motor modüllerinin birim testlerini koşar."
    dependsOn(subprojects.map { "${it.path}:test" })
}
