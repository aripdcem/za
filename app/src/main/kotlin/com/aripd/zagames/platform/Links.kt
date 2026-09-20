package com.aripd.zagames.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.aripd.zagames.R

/** Dışa açılan bağlantılar. Uygulama ağa çıkmaz; bağlantıyı cihazdaki tarayıcı açar. */
object ZaLinks {
    const val CONTACT = "zagames@aripd.com"
    const val SITE = "https://za.aripd.com"
    const val PRIVACY = "https://za.aripd.com/gizlilik.html"
    const val SOURCE = "https://github.com/aripdcem/za"
    const val REPORT = "https://github.com/aripdcem/za/issues/new"
    const val LICENSE = "https://github.com/aripdcem/za/blob/main/LICENSE"
    const val ZEMBEREK = "https://github.com/ahmetaa/zemberek-nlp"
    const val FREQUENCY_WORDS = "https://github.com/hermitdave/FrequencyWords"
    const val CC_BY_SA = "https://creativecommons.org/licenses/by-sa/4.0/"
    const val ANDROIDX = "https://developer.android.com/jetpack"
    const val KOTLIN = "https://kotlinlang.org"

    /**
     * E-posta uygulamasını adresi doldurulmuş olarak açar.
     *
     * ACTION_SENDTO + mailto: yalnızca e-posta uygulamalarını hedefler;
     * ACTION_VIEW tarayıcıyı da aday gösterirdi. Uygulama yine ağa çıkmaz,
     * mektubu kullanıcının kendi e-posta uygulaması gönderir.
     */
    fun email(context: Context, address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.about_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /** Bağlantıyı tarayıcıda açar; tarayıcı yoksa kısa bir uyarı gösterir. */
    fun open(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.about_open_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
