package com.udyneos.resteam.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.udyneos.resteam.domain.model.InfoRow
import com.udyneos.resteam.domain.repository.GameInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

class AndroidGameInfoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : GameInfoRepository {

    override fun getDeviceInfo(): List<InfoRow> = listOf(
        InfoRow("Perangkat", "${Build.MANUFACTURER} ${Build.MODEL}"),
        InfoRow("Android", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"),
        InfoRow("ABI", Build.SUPPORTED_ABIS.firstOrNull().orEmpty()),
    )

    override fun getAppInfo(packageName: String): List<InfoRow> {
        val info = packageInfo(packageName) ?: return listOf(InfoRow("Package", packageName))
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        return listOf(
            InfoRow("Package", packageName),
            InfoRow("Versi", "${info.versionName ?: "-"} (${versionCode(info)})"),
            InfoRow("Dipasang", dateFormat.format(Date(info.firstInstallTime))),
            InfoRow("Diperbarui", dateFormat.format(Date(info.lastUpdateTime))),
        )
    }

    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
}
