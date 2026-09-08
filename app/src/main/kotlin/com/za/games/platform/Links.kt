package com.za.games.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.za.games.R

/** Dışa açılan bağlantılar. Uygulama ağa çıkmaz; bağlantıyı cihazdaki tarayıcı açar. */
object ZaLinks {
    const val SITE = "https://za.aripd.com"
    const val PRIVACY = "https://za.aripd.com/gizlilik.html"
    const val SOURCE = "https://github.com/aripdcem/za"
    const val REPORT = "https://github.com/aripdcem/za/issues/new"
    const val ZEMBEREK = "https://github.com/ahmetaa/zemberek-nlp"
    const val FREQUENCY_WORDS = "https://github.com/hermitdave/FrequencyWords"
    const val CC_BY_SA = "https://creativecommons.org/licenses/by-sa/4.0/"
    const val ANDROIDX = "https://developer.android.com/jetpack"
    const val KOTLIN = "https://kotlinlang.org"

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
