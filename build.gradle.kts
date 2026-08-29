// ZA — zero ad game play platformu.
//
// Eklenti sürümleri gradle/libs.versions.toml kataloğunda pinlenir ve her
// modül kendi plugins bloğunda alias ile uygular. Kökte eklenti çözümlemesi
// yapılmaz; böylece Android SDK'sı olmayan ortamlarda da saf JVM modülleri
// (`:games:tetris` gibi) tek başına yapılandırılıp test edilebilir:
//
//   ./gradlew --configure-on-demand :games:tetris:test
