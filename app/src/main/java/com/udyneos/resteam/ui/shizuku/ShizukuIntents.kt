package com.udyneos.resteam.ui.shizuku

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val SHIZUKU_SITE = "https://shizuku.rikka.app"

/** Buka aplikasi Shizuku jika terpasang, jika tidak buka situs resminya. */
fun openShizukuApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_SITE))
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
