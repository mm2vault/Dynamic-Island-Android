package com.mm2vault.dynamicisland.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ReleaseInfo(val build: Int, val tag: String, val apkUrl: String, val sumsUrl: String?)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data class Ready(val file: File) : UpdateState
    data class Failed(val reason: Reason) : UpdateState
}

enum class Reason { NETWORK, NO_APK, CHECKSUM, STORAGE }

/** Pure helpers, unit tested. */
object UpdateRules {
    /** Tags look like "build-19". Returns null for anything else. */
    fun buildOf(tag: String?): Int? {
        if (tag == null || !tag.startsWith("build-")) return null
        return tag.removePrefix("build-").toIntOrNull()
    }

    fun isNewer(remote: Int, installed: Long): Boolean = remote.toLong() > installed

    /** SHA256SUMS lines look like "<hex>  <path>". */
    fun expectedHash(sums: String, fileName: String): String? =
        sums.lineSequence().map { it.trim() }
            .firstOrNull { it.isNotEmpty() && it.split(Regex("\\s+")).last().substringAfterLast('/') == fileName }
            ?.substringBefore(' ')?.lowercase()?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
}

object Updater {
    private const val API = "https://api.github.com/repos/mm2vault/Dynamic-Island-Android/releases/latest"
    private const val APK_NAME = "Dynamic-Island-Full.apk"
    private const val SUMS_NAME = "SHA256SUMS.txt"

    fun installedBuild(context: Context): Long =
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    /** Network call, runs only when the user presses the button. */
    suspend fun check(context: Context): UpdateState = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(httpGet(API))
            val tag = json.optString("tag_name")
            val build = UpdateRules.buildOf(tag) ?: return@withContext UpdateState.Failed(Reason.NO_APK)
            var apk: String? = null
            var sums: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                when (a.optString("name")) {
                    APK_NAME -> apk = a.optString("browser_download_url")
                    SUMS_NAME -> sums = a.optString("browser_download_url")
                }
            }
            if (!UpdateRules.isNewer(build, installedBuild(context))) return@withContext UpdateState.UpToDate
            if (apk.isNullOrEmpty()) return@withContext UpdateState.Failed(Reason.NO_APK)
            UpdateState.Available(ReleaseInfo(build, tag, apk, sums))
        } catch (e: Exception) {
            UpdateState.Failed(Reason.NETWORK)
        }
    }

    suspend fun download(context: Context, r: ReleaseInfo, onProgress: (Int) -> Unit): UpdateState = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, APK_NAME)
            val digest = MessageDigest.getInstance("SHA-256")
            val conn = open(r.apkUrl)
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { os ->
                    val buf = ByteArray(32 * 1024)
                    var done = 0L
                    var last = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        os.write(buf, 0, n); digest.update(buf, 0, n); done += n
                        if (total > 0) {
                            val p = (done * 100 / total).toInt()
                            if (p != last) { last = p; onProgress(p) }
                        }
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val expected = r.sumsUrl?.let { UpdateRules.expectedHash(httpGet(it), APK_NAME) }
            if (expected == null || expected != actual) {
                out.delete()
                return@withContext UpdateState.Failed(Reason.CHECKSUM)
            }
            UpdateState.Ready(out)
        } catch (e: java.io.IOException) {
            UpdateState.Failed(if (e is java.net.UnknownHostException || e is java.net.SocketException || e is java.net.SocketTimeoutException) Reason.NETWORK else Reason.STORAGE)
        } catch (e: Exception) {
            UpdateState.Failed(Reason.NETWORK)
        }
    }

    /** Opens Android's own installer. Android refuses it unless the signing key matches the installed app. */
    fun install(context: Context, file: File): Intent? {
        if (!context.packageManager.canRequestPackageInstalls()) {
            return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DynamicIsland-Updater")
            if (responseCode !in 200..299) throw java.io.IOException("HTTP $responseCode")
        }

    private fun httpGet(url: String): String = open(url).inputStream.bufferedReader().use { it.readText() }
}
