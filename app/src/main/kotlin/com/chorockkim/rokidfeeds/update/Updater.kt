package com.chorockkim.rokidfeeds.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import com.chorockkim.rokidfeeds.BuildConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * OTA 업데이트. rokid-lens·rokid-youtube와 같은 흐름이다: rokid-conversate 서버(server-v2)의 이 앱 폴더
 * ([BuildConfig.UPDATE_URL]: `/latest` = 버전 JSON, `/download` = APK, scripts/release.sh가 올림)에서
 * 확인하고, 받고, 검증하고, 시스템 설치기에 넘긴다.
 *
 * 처음 한 번 `adb install -i com.chorockkim.rokidfeeds`로 설치하면(이 앱이 자기 설치자) 다음부터는 안경에서
 * 아무것도 누르지 않고 업데이트된다(USER_ACTION_NOT_REQUIRED, targetSdk 29 이상 필요). 설치 뒤에는
 * [InstallResultReceiver]가 앱을 다시 띄운다.
 *
 * 설치 전 검증: 파일 SHA-256이 릴리스와 같아야 하고, APK가 이 앱(같은 패키지)이며 더 새 버전이고 설치된 앱과
 * 같은 키로 서명돼 있어야 한다. 다른 키는 시스템이 어차피 거부하지만 먼저 확인하면 이유를 분명히 보여 줄 수 있다.
 *
 * APK는 수 MB라 기사용 [com.chorockkim.rokidfeeds.data.BoundedHttp](본문 4MiB 상한, 문자열로 읽음) 대신
 * 스트리밍으로 받는다. 주소는 빌드 시 고정한 우리 서버 하나뿐이다.
 */
class Updater(private val context: Context) {
    sealed class Check {
        data class Available(val release: Release) : Check()
        data object UpToDate : Check()
    }

    class UpdateException(message: String) : IOException(message)

    /** 마지막 확인에서 찾은, 이 앱보다 새 릴리스. */
    var available: Release? = null
        private set

    /** 앱을 켤 때마다 확인한다(다른 Rokid 앱과 같음). 404 = 아직 올린 것이 없음. */
    suspend fun check(): Check = withContext(Dispatchers.IO) {
        val connection = open("${BuildConfig.UPDATE_URL}/latest")
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                available = null
                return@withContext Check.UpToDate
            }
            if (code !in 200..299) throw UpdateException("업데이트 확인 실패 (HTTP $code)")
            val json = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            available = Release.parse(json)?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
            available?.let { Check.Available(it) } ?: Check.UpToDate
        } finally {
            connection.disconnect()
        }
    }

    /** APK를 받아 검증한다. [onProgress]는 0..100(크기를 모르면 -1)을 메인 스레드에서 받는다. */
    suspend fun download(release: Release, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates")
        dir.deleteRecursively()
        dir.mkdirs()
        val file = File(dir, "rokid-feeds-${release.versionName}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = open("${BuildConfig.UPDATE_URL}/download")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw UpdateException("받기 실패 (HTTP $code)")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.size
            var done = 0L
            var lastPercent = -2
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        val percent = if (total > 0) (done * 100 / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            withContext(Dispatchers.Main) { onProgress(percent) }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != release.sha256) {
            file.delete()
            throw UpdateException("파일이 손상됐어요 (SHA-256 불일치)")
        }
        verifyApk(file, release)
        file
    }

    private fun verifyApk(file: File, release: Release) {
        val pm = context.packageManager
        val archive = archiveInfo(pm, file.path) ?: throw UpdateException("APK를 읽지 못했어요")
        if (archive.packageName != context.packageName) throw UpdateException("다른 앱의 APK예요")
        @Suppress("DEPRECATION")
        val archiveCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        if (archiveCode <= BuildConfig.VERSION_CODE) throw UpdateException("지금 버전보다 새 버전이 아니에요")
        if (archiveCode != release.versionCode.toLong()) throw UpdateException("릴리스와 APK 버전이 달라요")
        val installed = signatures(pm.getPackageInfo(context.packageName, signingFlags()))
        val incoming = signatures(archive)
        if (installed.isEmpty() || installed != incoming) throw UpdateException("서명 키가 달라서 덮어 설치할 수 없어요")
    }

    /** 이 앱이 APK를 설치해도 되는지("이 출처 허용"). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** "이 출처 허용"을 켜는 시스템 화면. */
    fun installPermissionIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:${context.packageName}"))

    /** 시스템 설치 세션을 시작한다. 결과는 [InstallResultReceiver]로 온다. */
    suspend fun install(apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, InstallResultReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, id, intent, flags).intentSender)
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = DOWNLOAD_READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "rokid-feeds-updater")
        }

    companion object {
        private const val TIMEOUT_MS = 10_000
        /** 서버가 잠들어 있으면(Railway App Sleeping) 첫 응답이 늦다. */
        private const val DOWNLOAD_READ_TIMEOUT_MS = 60_000

        @Suppress("DEPRECATION")
        private fun signingFlags() =
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

        private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? =
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(signingFlags().toLong()))
            } else {
                @Suppress("DEPRECATION") pm.getPackageArchiveInfo(path, signingFlags())
            }

        @Suppress("DEPRECATION")
        private fun signatures(info: PackageInfo): Set<String> {
            val sigs = if (Build.VERSION.SDK_INT >= 28) {
                info.signingInfo?.let { if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory }
            } else {
                info.signatures
            }
            return sigs.orEmpty().map { sig ->
                MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
            }.toSet()
        }
    }
}
