package cn.leftshine.apkexport.update

import cn.leftshine.apkexport.R

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import cn.leftshine.apkexport.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal enum class CheckStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, FAILED }
internal enum class DownloadStatus { NOT_STARTED, DOWNLOADING, VERIFYING, READY, NEEDS_AUTHORIZATION, LAUNCHING, FAILED }

internal data class UpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val publishedAt: Instant?,
    val changelog: String,
    val maxForcedVersionCode: Long,
    val policyRevision: Long,
    val urls: List<String>,
    val sha256: String,
    val size: Long?,
) {
    fun isForced(current: Long) = current <= maxForcedVersionCode && versionCode > current
    fun localPublishedAt(): String? = publishedAt?.let {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(it)
    }
}

internal data class UpdateUiState(
    val checkStatus: CheckStatus = CheckStatus.IDLE,
    val manifest: UpdateManifest? = null,
    val showDialog: Boolean = false,
    val hasUpdateDot: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_STARTED,
    val progress: Int? = null,
    val error: String? = null,
)

internal object AppUpdateManager {
    private const val PREFS = "app_update"
    private const val KEY_SKIPPED = "skipped_version"
    private const val KEY_REVISION = "highest_policy_revision"
    private const val CHANNEL = "stable"
    private const val MAX_METADATA_BYTES = 1024 * 1024
    private const val USER_AGENT = "APKExport/${BuildConfig.VERSION_NAME} Android"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val checking = AtomicBoolean(false)
    private val autoStarted = AtomicBoolean(false)
    private var manualRequested = false
    private var appContext: Context? = null
    private var verifiedApk: File? = null
    private val mutableState = MutableStateFlow(UpdateUiState())
    private val mutableEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()
    val events: SharedFlow<String> = mutableEvents.asSharedFlow()

    private data class Source(val url: String, val authority: Boolean = false)
    private data class Candidate(val manifest: UpdateManifest, val digest: String, val authority: Boolean, val host: String)

    private val sources = listOf(
        Source("https://api.github.com/repos/daxiaamu/APKExport/contents/update.json?ref=master", true),
        Source("https://raw.githubusercontent.com/daxiaamu/APKExport/master/update.json"),
        Source("https://cdn.jsdelivr.net/gh/daxiaamu/APKExport@master/update.json"),
        Source("https://fastly.jsdelivr.net/gh/daxiaamu/APKExport@master/update.json"),
        Source("https://gcore.jsdelivr.net/gh/daxiaamu/APKExport@master/update.json"),
        Source("https://testingcf.jsdelivr.net/gh/daxiaamu/APKExport@master/update.json"),
        Source("https://cdn.statically.io/gh/daxiaamu/APKExport/master/update.json"),
    )

    fun initialize(context: Context) { appContext = context.applicationContext }

    fun autoCheck() {
        if (autoStarted.compareAndSet(false, true)) check(manual = false)
    }

    fun manualCheck() = check(manual = true)

    @Synchronized
    private fun check(manual: Boolean) {
        val context = appContext ?: return
        if (checking.get()) {
            if (manual) manualRequested = true
            return
        }
        checking.set(true)
        manualRequested = manual
        mutableState.value = mutableState.value.copy(checkStatus = CheckStatus.CHECKING, error = null)
        scope.launch {
            val result = runCatching { selectTrustedManifest(context) }
            val shouldReportManual = synchronized(this@AppUpdateManager) { manualRequested.also { manualRequested = false } }
            result.onSuccess { manifest -> handleCheckResult(context, manifest, shouldReportManual) }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(checkStatus = CheckStatus.FAILED, error = error.message)
                    if (shouldReportManual) mutableEvents.emit(context.getString(R.string.update_check_failed))
                }
            checking.set(false)
        }
    }

    private suspend fun selectTrustedManifest(context: Context): UpdateManifest {
        val candidates = coroutineScope { sources.map { source -> async { fetchCandidate(source) } }.awaitAll() }.filterNotNull()
        check(candidates.isNotEmpty()) { "所有更新源均不可用" }
        val highestAccepted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_REVISION, 0)
        val eligible = candidates.filter { it.manifest.policyRevision >= highestAccepted }
        check(eligible.isNotEmpty()) { "检测到旧版更新策略，已拒绝" }
        val authority = eligible.firstOrNull { it.authority }
        val selected = authority ?: eligible.groupBy { "${it.manifest.policyRevision}:${it.digest}" }
            .values.filter { group -> group.map { it.host }.distinct().size >= 2 }
            .maxByOrNull { it.first().manifest.policyRevision }?.first()
        checkNotNull(selected) { "镜像结果不足以形成可信共识" }
        val conflicts = eligible.filter { it.manifest.policyRevision == selected.manifest.policyRevision && it.digest != selected.digest }
        check(conflicts.isEmpty()) { "同一策略版本的元数据存在冲突" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_REVISION, selected.manifest.policyRevision)
        }
        return selected.manifest
    }

    private suspend fun fetchCandidate(source: Source): Candidate? = withContext(Dispatchers.IO) {
        runCatching {
            val separator = if (source.url.contains('?')) '&' else '?'
            val connection = URL(source.url + separator + "t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
            connection.connectTimeout = 7_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
            check(connection.url.protocol == "https") { "不安全的重定向" }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                while (output.size() <= MAX_METADATA_BYTES) {
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_METADATA_BYTES + 1 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            check(bytes.size <= MAX_METADATA_BYTES) { "元数据超过 1 MiB" }
            val raw = bytes.toString(Charsets.UTF_8)
            val jsonText = if (source.authority) {
                val wrapper = JSONObject(raw)
                check(wrapper.optString("encoding") == "base64") { "权威源编码无效" }
                Base64.decode(wrapper.getString("content"), Base64.DEFAULT).toString(Charsets.UTF_8)
            } else raw
            val manifest = parseManifest(JSONObject(jsonText))
            val canonical = buildString {
                append(manifest.policyRevision).append('|').append(manifest.versionCode).append('|')
                append(manifest.maxForcedVersionCode).append('|').append(manifest.sha256).append('|')
                append(manifest.urls.joinToString(","))
            }
            Candidate(manifest, sha256(canonical.toByteArray()), source.authority, URI(source.url).host)
        }.getOrNull()
    }

    private fun parseManifest(json: JSONObject): UpdateManifest {
        check(json.getInt("schemaVersion") == 1) { "不支持的 schemaVersion" }
        check(json.getString("channel") == CHANNEL) { "更新渠道不匹配" }
        val versionCode = json.getLong("versionCode")
        val versionName = json.getString("versionName").trim()
        val forced = json.getLong("maxForcedVersionCode")
        val revision = json.getLong("policyRevision")
        val sha = json.optString("sha256", json.optString("apkSha256")).lowercase()
        check(versionCode > 0 && versionName.isNotBlank()) { "版本字段无效" }
        check(sha.matches(Regex("[0-9a-f]{64}"))) { "SHA-256 无效" }
        check(forced >= 0 && forced < versionCode) { "强制更新边界无效" }
        check(revision >= 0) { "policyRevision 无效" }
        val urls = buildList {
            val array = json.optJSONArray("urls") ?: json.optJSONArray("apkUrls")
            if (array != null) for (index in 0 until array.length()) add(array.getString(index))
            val single = json.optString("url", json.optString("apkUrl"))
            if (single.isNotBlank()) add(single)
        }.distinct()
        check(urls.size >= 5) { "APK 下载源少于 5 个" }
        check(urls.all { runCatching { URI(it).scheme == "https" }.getOrDefault(false) }) { "APK 地址必须为 HTTPS" }
        check(urls.map { URI(it).host.lowercase() }.distinct().size >= 5) { "APK 下载源主机少于 5 个" }
        val publishedAt = json.optString("publishedAt").takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return UpdateManifest(
            versionCode, versionName, publishedAt,
            json.optString("changelog", json.optString("notes")),
            forced, revision, urls, sha,
            json.optLong("size").takeIf { json.has("size") && it > 0 },
        )
    }

    private suspend fun handleCheckResult(context: Context, manifest: UpdateManifest, manual: Boolean) {
        val current = BuildConfig.VERSION_CODE.toLong()
        if (manifest.versionCode <= current) {
            mutableState.value = UpdateUiState(checkStatus = CheckStatus.UP_TO_DATE)
            if (manual) mutableEvents.emit(context.getString(R.string.update_latest))
            return
        }
        val skipped = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SKIPPED, 0)
        val shouldShow = manual || manifest.isForced(current) || skipped != manifest.versionCode
        mutableState.value = UpdateUiState(
            checkStatus = CheckStatus.AVAILABLE,
            manifest = manifest,
            showDialog = shouldShow,
            hasUpdateDot = !shouldShow,
        )
        if (manual && !shouldShow) mutableEvents.emit(context.getString(R.string.update_found, manifest.versionName))
    }

    fun ignore() {
        val forced = mutableState.value.manifest?.isForced(BuildConfig.VERSION_CODE.toLong()) == true
        if (!forced) mutableState.value = mutableState.value.copy(showDialog = false, hasUpdateDot = true)
    }

    fun skip() {
        val context = appContext ?: return
        val manifest = mutableState.value.manifest ?: return
        if (manifest.isForced(BuildConfig.VERSION_CODE.toLong())) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putLong(KEY_SKIPPED, manifest.versionCode) }
        mutableState.value = mutableState.value.copy(showDialog = false, hasUpdateDot = false)
    }

    fun download() {
        val context = appContext ?: return
        val manifest = mutableState.value.manifest ?: return
        if (mutableState.value.downloadStatus == DownloadStatus.DOWNLOADING) return
        scope.launch {
            mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.DOWNLOADING, progress = 0, error = null)
            val result = runCatching { downloadFromMirrors(context, manifest) }
            result.onSuccess { file ->
                verifiedApk = file
                mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.READY, progress = 100)
            }.onFailure {
                mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.FAILED, progress = null, error = context.getString(R.string.update_download_failed))
            }
        }
    }

    private fun downloadFromMirrors(context: Context, manifest: UpdateManifest): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "update-${manifest.versionCode}.apk")
        var lastError: Throwable? = null
        for (url in manifest.urls) {
            val temporary = File(directory, "update-${manifest.versionCode}.download")
            temporary.delete()
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.instanceFollowRedirects = true
                check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
                check(connection.url.protocol == "https") { "下载被重定向到非 HTTPS" }
                val total = if (Build.VERSION.SDK_INT >= 24) connection.contentLengthLong else connection.contentLength.toLong()
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var copied = 0L
                        var lastProgressAt = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            copied += count
                            val now = System.currentTimeMillis()
                            if (now - lastProgressAt >= 300) {
                                val progress = if (total > 0) ((copied * 100 / total).toInt().coerceAtMost(99)) else null
                                mutableState.value = mutableState.value.copy(progress = progress)
                                lastProgressAt = now
                            }
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
                mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.VERIFYING, progress = 99)
                check(digest.digest().toHex().secureEquals(manifest.sha256)) { "SHA-256 校验失败" }
                target.delete()
                check(temporary.renameTo(target)) { "无法保存已验证安装包" }
                return target
            } catch (error: Throwable) {
                temporary.delete()
                lastError = error
            }
        }
        throw IllegalStateException("所有下载源均失败", lastError)
    }

    fun install(activity: Activity) {
        val context = appContext ?: return
        val manifest = mutableState.value.manifest ?: return
        scope.launch {
            val result = runCatching { verifyInstallIdentity(context, manifest) }
            result.onFailure {
                verifiedApk?.delete(); verifiedApk = null
                mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.FAILED, error = context.getString(R.string.update_verify_failed))
            }.onSuccess { file ->
                if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                    mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.NEEDS_AUTHORIZATION)
                    withContext(Dispatchers.Main) {
                        activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()))
                    }
                } else {
                    launchInstaller(activity, file)
                }
            }
        }
    }

    private fun verifyInstallIdentity(context: Context, manifest: UpdateManifest): File {
        val file = checkNotNull(verifiedApk) { "安装包不存在" }
        check(file.isFile && sha256(file).secureEquals(manifest.sha256)) { "SHA-256 不匹配" }
        val archive = archiveInfo(context.packageManager, file)
        check(archive.packageName == context.packageName) { "包名不匹配" }
        check(packageVersion(archive) == manifest.versionCode) { "versionCode 不匹配" }
        val installed = installedInfo(context.packageManager, context.packageName)
        check(certificates(archive) == certificates(installed)) { "签名证书不匹配" }
        return file
    }

    private fun launchInstaller(activity: Activity, file: File) {
        mutableState.value = mutableState.value.copy(downloadStatus = DownloadStatus.LAUNCHING)
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, ApkRepositoryMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, file: File): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return checkNotNull(pm.getPackageArchiveInfo(file.path, flags)) { "无法读取 APK 信息" }
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager, packageName: String): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return pm.getPackageInfo(packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun certificates(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty() else info.signatures.orEmpty()
        return signatures.map { sha256(it.toByteArray()) }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun packageVersion(info: PackageInfo) = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(256 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.secureEquals(other: String) = MessageDigest.isEqual(toByteArray(), other.toByteArray())
    private const val ApkRepositoryMime = "application/vnd.android.package-archive"
}