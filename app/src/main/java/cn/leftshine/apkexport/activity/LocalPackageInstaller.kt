package cn.leftshine.apkexport.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import cn.leftshine.apkexport.R
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object LocalPackageInstaller {
    private const val APK_MIME = "application/vnd.android.package-archive"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun install(context: Context, file: LocalApkFile) {
        if (file.name.endsWith(".apk", ignoreCase = true)) {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(file.uri, APK_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        }
        val appContext = context.applicationContext
        scope.launch {
            runCatching { installArchive(appContext, file) }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, appContext.getString(R.string.local_package_install_failed, error.message.orEmpty()), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installArchive(context: Context, file: LocalApkFile) {
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
        val session = installer.openSession(sessionId)
        try {
            var apkCount = 0
            context.contentResolver.openInputStream(file.uri).use { source ->
                checkNotNull(source) { context.getString(R.string.local_package_open_failed) }
                ZipInputStream(source.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                            session.openWrite("split_${apkCount++}.apk", 0, entry.size.coerceAtLeast(0)).use { output ->
                                zip.copyTo(output)
                                session.fsync(output)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            check(apkCount > 0) { context.getString(R.string.local_package_no_apk_entries) }
            val callback = Intent(context, PackageInstallReceiver::class.java).setAction(PackageInstallReceiver.ACTION_INSTALL_STATUS)
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, callback, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            session.commit(pendingIntent.intentSender)
        } catch (error: Throwable) {
            runCatching { session.abandon() }
            throw error
        } finally {
            session.close()
        }
    }
}