package com.aripd.zagames.platform

import android.content.Context
import com.aripd.zagames.sozluk.WordLang

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

    /** Oyuncunun seçtiği dil, ya da null: arayüzün dilini izle. */
    fun chosen(context: Context): WordLang? = WordLang.of(SettingsStore(context).wordLanguage)

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
        SettingsStore(context).wordLanguage = lang?.tag ?: ""
    }

}
