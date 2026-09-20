package com.aripd.zagames.platform

import android.content.Context
import com.aripd.zagames.sozluk.WordLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kelime oyunlarının dili ile arayüzün dili arasındaki köprü.
 *
 * İkisi ayrı tutulur. Arayüz dili telefonun ayarıdır; kelime dili oyunun
 * sözlüğüdür ve oyuncu onu oyun içinden ayrıca seçebilir. Almanya'daki bir
 * oyuncu uygulamayı Almanca kullanıp Beş Harf'i Türkçe oynayabilsin diye.
 *
 * Seçim yapılmadıysa arayüzün dili denenir; o dilin kelime listesi yoksa
 * İngilizceye düşülür — arayüz o dilde kalmaya devam eder.
 */
object WordLangs {

    /**
     * Seçilen dilin etiketi, uygulama boyunca tek kopya. Yalnız kayıttan
     * okumak yetmiyordu: kurulum kartındaki seçici yazdığında oyunun görünüm
     * modeli haberdar oluyor ama klavyeyi besleyen [LocalWordLang] olmuyordu,
     * çünkü onu kuran `remember` yalnız arayüz dilini izliyordu. Sözlük yeni
     * dile geçerken klavye eskisinde kalıyordu (v0.43.0 cihaz koşumu, F1).
     * Akış sayesinde iki taraf aynı anda uyanıyor.
     */
    private val chosenTag = MutableStateFlow<String?>(null)

    /** Seçim değiştikçe yayar; arayüz katmanı buna abone olur. */
    val selection: StateFlow<String?> = chosenTag.asStateFlow()

    /** Oyuncunun seçtiği dil, ya da null: arayüzün dilini izle. */
    fun chosen(context: Context): WordLang? = WordLang.of(tag(context))

    /** Kayıttan bir kez okunur, sonrası akıştan gelir. */
    private fun tag(context: Context): String =
        chosenTag.value ?: SettingsStore(context).wordLanguage.also { chosenTag.value = it }

    /** Oyunun şu anda oynanacağı dil. */
    fun current(context: Context): WordLang = chosen(context) ?: followed(context)

    /**
     * Arayüzün dilinin karşılığı: seçim yapılmadığında oynanacak dil, ve
     * seçicideki "arayüzün dili" satırının altında gösterilen ad.
     */
    fun followed(context: Context): WordLang =
        WordLang.forTagOrDefault(
            ZaLocale.normalize(context.resources.configuration.locales[0]) ?: "",
        )

    /** Seçimi saklar; boş etiket "arayüzün dilini izle" demektir. */
    fun choose(context: Context, lang: WordLang?) {
        val tag = lang?.tag ?: ""
        SettingsStore(context).wordLanguage = tag
        chosenTag.value = tag
    }

}
