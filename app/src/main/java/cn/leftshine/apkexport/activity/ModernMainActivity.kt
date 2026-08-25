package cn.leftshine.apkexport.activity

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ContentUris
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.leftshine.apkexport.BuildConfig
import cn.leftshine.apkexport.R
import cn.leftshine.apkexport.update.AppUpdateManager
import cn.leftshine.apkexport.utils.Settings
import cn.leftshine.apkexport.update.UpdateAction
import cn.leftshine.apkexport.update.UpdateDialogHost
import java.io.File
import java.io.FileInputStream
import java.text.Collator
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class ModernMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialDestination = MainDestination.entries.getOrElse(intent.getIntExtra(EXTRA_DESTINATION, MainDestination.EXPORT.ordinal)) { MainDestination.EXPORT }
        setContent { ApkExportApp(initialDestination) }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_EXPORT = 0
        const val DESTINATION_SETTINGS = 2
    }
}

private enum class MainDestination { EXPORT, LOCAL_APK, SETTINGS }
private enum class AppSort { NAME, PACKAGE, SIZE, INSTALLED, UPDATED }
private enum class ThemeMode { SYSTEM, LIGHT, DARK }

internal data class InstalledApp(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sourcePath: String,
    val size: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystem: Boolean,
)

internal data class LocalApkFile(val name: String, val uri: Uri, val path: String, val size: Long, val modifiedAt: Long)

internal data class MainUiState(
    val apps: List<InstalledApp> = emptyList(),
    val localApks: List<LocalApkFile> = emptyList(),
    val loading: Boolean = true,
    val localApksLoading: Boolean = false,
    val exportingPackage: String? = null,
)

internal data class ExportedApk(val uri: Uri, val displayPath: String)

internal class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ApkRepository(application)
    private val mutableState = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true)
            val apps = withContext(Dispatchers.IO) { repository.loadApps() }
            mutableState.value = mutableState.value.copy(apps = apps, loading = false)
        }
    }

    fun refreshLocalApks() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(localApksLoading = true)
            val files = withContext(Dispatchers.IO) { runCatching { repository.loadLocalApks() } }
            mutableState.value = files.fold(
                onSuccess = { mutableState.value.copy(localApks = it, localApksLoading = false) },
                onFailure = { mutableState.value.copy(localApksLoading = false) },
            )
        }
    }

    fun deleteLocalApk(file: LocalApkFile) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteLocalApk(file) }
            refreshLocalApks()
        }
    }
    fun renameLocalApk(file: LocalApkFile, requestedName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.renameLocalApk(file, requestedName) } }
            refreshLocalApks()
        }
    }
    suspend fun export(app: InstalledApp, requestedFileName: String? = null): Result<ExportedApk> {
        mutableState.value = mutableState.value.copy(exportingPackage = app.packageName)
        return try {
            withContext(Dispatchers.IO) { Result.success(repository.export(app, requestedFileName)) }
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            mutableState.value = mutableState.value.copy(exportingPackage = null)
        }
    }
    suspend fun exportBatch(
        apps: List<InstalledApp>,
        parallelism: Int = 3,
        onProgress: (completed: Int) -> Unit,
    ): List<Result<ExportedApk>> = coroutineScope {
        val semaphore = Semaphore(parallelism.coerceAtLeast(1))
        val completed = AtomicInteger(0)
        apps.map { app ->
            async(Dispatchers.IO) {
                val result = semaphore.withPermit { runCatching { repository.export(app) } }
                onProgress(completed.incrementAndGet())
                result
            }
        }.awaitAll()
    }
}

internal class ApkRepository(private val context: Context) {
    private val packageManager = context.packageManager

    fun loadApps(): List<InstalledApp> {
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }
        val collator = Collator.getInstance(Locale.getDefault())
        return packages.mapNotNull { packageInfo ->
            runCatching {
                val info = checkNotNull(packageInfo.applicationInfo)
                val source = File(info.sourceDir)
                InstalledApp(
                    label = info.loadLabel(packageManager).toString(),
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName.orEmpty().ifBlank { "—" },
                    versionCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    },
                    sourcePath = info.sourceDir,
                    size = source.length(),
                    firstInstallTime = packageInfo.firstInstallTime,
                    lastUpdateTime = packageInfo.lastUpdateTime,
                    isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }.getOrNull()
        }.sortedWith { left, right -> collator.compare(left.label, right.label) }
    }
    fun loadLocalApks(): List<LocalApkFile> {
        if (Build.VERSION.SDK_INT >= 29) {
            val canReadAllFiles = (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) ||
                (Build.VERSION.SDK_INT == 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            val collection = if (canReadAllFiles) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.VOLUME_NAME, MediaStore.Downloads.RELATIVE_PATH, MediaStore.Downloads.SIZE, MediaStore.Downloads.DATE_MODIFIED)
            val selection = INSTALL_PACKAGE_EXTENSIONS.joinToString(" OR ") { "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?" }
            val args = INSTALL_PACKAGE_EXTENSIONS.map { "%.$it" }.toTypedArray()
            return context.contentResolver.query(collection, projection, selection, args, "${MediaStore.Downloads.DATE_MODIFIED} DESC")?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val name = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val volume = cursor.getColumnIndexOrThrow(MediaStore.Downloads.VOLUME_NAME)
                val relativePath = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val modified = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                buildList {
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(name)
                        val volumeName = cursor.getString(volume)
                        val root = if (volumeName == MediaStore.VOLUME_EXTERNAL_PRIMARY) "/storage/emulated/0" else "/storage/$volumeName"
                        val path = "$root/${cursor.getString(relativePath).orEmpty()}$displayName"
                        add(LocalApkFile(displayName, ContentUris.withAppendedId(collection, cursor.getLong(id)), path, cursor.getLong(size), cursor.getLong(modified)))
                    }
                }
            }.orEmpty()
        }
        @Suppress("DEPRECATION")
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "APKExport")
        return directory.walkTopDown().filter { file -> file.isFile && file.extension.equals("apk", true) }.toList().sortedByDescending { it.lastModified() }.map { file ->
            LocalApkFile(file.name, androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), file.absolutePath, file.length(), file.lastModified() / 1000)
        }
    }

    fun deleteLocalApk(file: LocalApkFile) {
        if (file.uri.scheme == "content" && Build.VERSION.SDK_INT >= 29) context.contentResolver.delete(file.uri, null, null)
        else File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "APKExport/${file.name}").delete()
    }
    fun renameLocalApk(file: LocalApkFile, requestedName: String) {
        val extension = file.name.substringAfterLast('.', "apk").lowercase(Locale.ROOT)
        val baseName = requestedName.trim().removeSuffix(".$extension").trim()
        if (baseName.isBlank() || baseName.contains(Regex("[\\/:*?\"<>|]"))) return
        val newName = "$baseName.$extension"
        if (file.uri.scheme == "content" && Build.VERSION.SDK_INT >= 29) {
            check(context.contentResolver.update(file.uri, ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, newName) }, null, null) > 0) { context.getString(R.string.modern_rename_failed) }
        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "APKExport")
            check(File(directory, file.name).renameTo(File(directory, newName))) { context.getString(R.string.modern_rename_failed) }
        }
    }
    fun export(app: InstalledApp, requestedFileName: String? = null): ExportedApk {
        val format = Settings.getCustomFileNameFormat().orEmpty().ifBlank { "#N-#P-#V" }
        val generatedName = format.replace("#N", app.label).replace("#P", app.packageName).replace("#V", app.versionName).replace("#C", app.versionCode.toString())
        val rawFileName = requestedFileName ?: "$generatedName.apk"
        val fileName = rawFileName.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").trim().ifBlank { "${app.packageName}.apk" }
        return if (Build.VERSION.SDK_INT >= 29) exportToMediaStore(app, fileName) else exportLegacy(app, fileName)
    }

    private fun configuredExportDirectory(): File {
        val configured = File(Settings.getCustomExportPath().orEmpty())
        return if (configured.isAbsolute) configured else File(Environment.getExternalStorageDirectory(), configured.path)
    }
    private fun configuredRelativeExportPath(): String {
        val root = Environment.getExternalStorageDirectory()
        val directory = configuredExportDirectory()
        return runCatching { directory.relativeTo(root).invariantSeparatorsPath }.getOrNull()?.takeIf { it.isNotBlank() && !it.startsWith("..") }
            ?: "${Environment.DIRECTORY_DOWNLOADS}/APKExport"
    }
    @androidx.annotation.RequiresApi(29)
    private fun exportToMediaStore(app: InstalledApp, fileName: String): ExportedApk {
        val relativePath = configuredRelativeExportPath()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, APK_MIME)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(resolver.insert(collection, values)) { context.getString(R.string.modern_create_download_failed) }
        try {
            resolver.openOutputStream(uri, "w").use { output ->
                checkNotNull(output) { context.getString(R.string.modern_open_export_failed) }
                FileInputStream(app.sourcePath).use { input -> input.copyTo(output, 256 * 1024) }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            return ExportedApk(uri, "$relativePath/$fileName")
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun exportLegacy(app: InstalledApp, fileName: String): ExportedApk {
        val directory = configuredExportDirectory()
        check(directory.exists() || directory.mkdirs()) { context.getString(R.string.modern_create_export_dir_failed) }
        val destination = File(directory, fileName)
        FileInputStream(app.sourcePath).use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination)
        return ExportedApk(uri, "${directory.path}/$fileName")
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        val INSTALL_PACKAGE_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm")
    }
}

private object AppIconLoader {
    private val cache = LruCache<String, Bitmap>(64)

    suspend fun load(context: Context, packageName: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(packageName) ?: runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmapSafe().also { cache.put(packageName, it) }
        }.getOrNull()
    }
}
private object LocalPackageIconLoader {
    private val cache = LruCache<String, Bitmap>(96)

    suspend fun load(context: Context, file: LocalApkFile): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${file.uri}:${file.modifiedAt}:${file.size}"
        cache.get(key) ?: runCatching {
            val workDir = File(context.cacheDir, "local_package_icons").apply { mkdirs() }
            val source = File(workDir, "${key.hashCode()}.${file.name.substringAfterLast('.', "apk")}")
            context.contentResolver.openInputStream(file.uri).use { input ->
                checkNotNull(input)
                source.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
            }
            var apk = source
            try {
                apk = if (file.name.endsWith(".apk", ignoreCase = true)) source else extractBaseApk(source, workDir, key.hashCode())
                val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
                }
                val applicationInfo = checkNotNull(packageInfo?.applicationInfo).apply {
                    sourceDir = apk.absolutePath
                    publicSourceDir = apk.absolutePath
                }
                applicationInfo.loadIcon(context.packageManager).toBitmapSafe().also { cache.put(key, it) }
            } finally {
                if (apk != source) apk.delete()
                source.delete()
            }
        }.getOrNull()
    }

    private fun extractBaseApk(archive: File, workDir: File, hash: Int): File {
        val output = File(workDir, "$hash-base.apk")
        java.util.zip.ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".apk", true) }.toList()
            val entry = entries.firstOrNull { it.name.substringAfterLast('/').equals("base.apk", true) }
                ?: entries.firstOrNull { it.name.substringAfterLast('/').contains("base-master", true) }
                ?: entries.firstOrNull()
                ?: error("No APK in archive")
            zip.getInputStream(entry).use { input -> output.outputStream().use { sink -> input.copyTo(sink, 128 * 1024) } }
        }
        return output
    }
}
internal fun Drawable.toBitmapSafe(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    return createBitmap(width, height).also { bitmap ->
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }
}

@Composable
private fun ApkExportApp(initialDestination: MainDestination, viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val systemDarkMode = isSystemInDarkTheme()
    val themePreferences = remember { context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE) }
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.entries.getOrElse(themePreferences.getInt("theme_mode", ThemeMode.SYSTEM.ordinal)) { ThemeMode.SYSTEM }) }
    val darkMode = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkMode
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var pendingApp by remember { mutableStateOf<InstalledApp?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var pendingShare by remember { mutableStateOf(false) }
    var pendingBatchShare by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var pendingBatchExport by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var batchExportTotal by remember { mutableStateOf(0) }
    var batchExportCompleted by remember { mutableStateOf(0) }
    var batchExportFailures by remember { mutableStateOf(0) }
    var batchExportRunning by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        AppUpdateManager.initialize(context)
        delay(2_000)
        AppUpdateManager.autoCheck()
    }
    LaunchedEffect(Unit) {
        AppUpdateManager.events.collect { snackbar.showSnackbar(it) }
    }
    val localAccessPreferences = remember { context.getSharedPreferences("local_apk_access", Context.MODE_PRIVATE) }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()) viewModel.refreshLocalApks()
        else scope.launch { snackbar.showSnackbar(resources.getString(R.string.modern_all_files_denied)) }
    }
    val readStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.refreshLocalApks()
        else scope.launch { snackbar.showSnackbar(resources.getString(R.string.modern_storage_denied)) }
    }
    fun requestLocalApkAccess() {
        when {
            Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager() -> {
                if (!localAccessPreferences.getBoolean("requested", false)) {
                    localAccessPreferences.edit { putBoolean("requested", true) }
                    allFilesAccessLauncher.launch(Intent(AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${context.packageName}".toUri()))
                } else viewModel.refreshLocalApks()
            }
            Build.VERSION.SDK_INT == 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED -> {
                if (!localAccessPreferences.getBoolean("requested", false)) {
                    localAccessPreferences.edit { putBoolean("requested", true) }
                    readStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else viewModel.refreshLocalApks()
            }
            else -> viewModel.refreshLocalApks()
        }
    }

    suspend fun exportAll(apps: List<InstalledApp>) {
        if (apps.isEmpty() || batchExportRunning) return
        batchExportTotal = apps.size
        batchExportCompleted = 0
        batchExportFailures = 0
        batchExportRunning = true
        val results = viewModel.exportBatch(apps) { completed ->
            batchExportCompleted = completed
        }
        batchExportFailures = results.count { it.isFailure }
        batchExportRunning = false
        snackbar.showSnackbar(
            resources.getString(
                R.string.modern_batch_export_result,
                batchExportTotal - batchExportFailures,
                batchExportTotal,
                batchExportFailures,
            ),
        )
    }

    suspend fun exportAndShareAll(apps: List<InstalledApp>) {
        val exported = apps.mapNotNull { app -> viewModel.export(app).getOrNull() }
        if (exported.isNotEmpty()) shareApks(context, exported)
        else snackbar.showSnackbar(resources.getString(R.string.modern_share_failed, resources.getString(R.string.modern_unknown_error)))
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val app = pendingApp
        val batch = pendingBatchShare
        val exportBatch = pendingBatchExport
        val requestedFileName = pendingFileName
        val share = pendingShare
        pendingApp = null
        pendingFileName = null
        pendingBatchShare = emptyList()
        pendingBatchExport = emptyList()
        if (granted && exportBatch.isNotEmpty()) {
            scope.launch { exportAll(exportBatch) }
        } else if (granted && batch.isNotEmpty()) {
            scope.launch { exportAndShareAll(batch) }
        } else if (granted && app != null) {
            scope.launch {
                if (share) viewModel.export(app, requestedFileName).onSuccess { shareApk(context, it) }.onFailure { snackbar.showSnackbar(resources.getString(R.string.modern_share_failed, it.message ?: resources.getString(R.string.modern_unknown_error))) }
                else exportAndReport(context, viewModel, app, snackbar, requestedFileName)
            }
        } else if (!granted) {
            scope.launch { snackbar.showSnackbar(resources.getString(R.string.modern_legacy_storage_required)) }
        }
    }
    fun requestAction(app: InstalledApp, requestedFileName: String?, share: Boolean) {
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingApp = app; pendingFileName = requestedFileName; pendingShare = share
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else scope.launch {
            if (share) viewModel.export(app, requestedFileName).onSuccess { shareApk(context, it) }.onFailure { snackbar.showSnackbar(resources.getString(R.string.modern_share_failed, it.message ?: resources.getString(R.string.modern_unknown_error))) }
            else exportAndReport(context, viewModel, app, snackbar, requestedFileName)
        }
    }
    val requestExport: (InstalledApp) -> Unit = { requestAction(it, null, false) }
    val requestCustomExport: (InstalledApp, String) -> Unit = { app, name -> requestAction(app, name, false) }
    val requestShare: (InstalledApp) -> Unit = { requestAction(it, null, true) }
    val requestCustomShare: (InstalledApp, String) -> Unit = { app, name -> requestAction(app, name, true) }
    val requestBatchExport: (List<InstalledApp>) -> Unit = { apps ->
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingBatchExport = apps
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else scope.launch { exportAll(apps) }
    }
    val requestBatchShare: (List<InstalledApp>) -> Unit = { apps ->
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingBatchShare = apps
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else scope.launch { exportAndShareAll(apps) }
    }
    val colors = when {
        Build.VERSION.SDK_INT >= 31 && darkMode -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        darkMode -> darkColorScheme(primary = Color(0xFF8ED69B), secondary = Color(0xFFB8CCB9), surface = Color(0xFF151A16), background = Color(0xFF0E120F))
        else -> lightColorScheme(primary = Color(0xFF176B3A), secondary = Color(0xFF506353), surface = Color(0xFFF8FAF7), background = Color(0xFFF2F5F1))
    }

    MaterialTheme(colorScheme = colors) {
        MainScreen(
            state = state,
            snackbar = snackbar,
            initialDestination = initialDestination,
            darkMode = darkMode,
            themeMode = themeMode,
            onThemeModeChange = { mode ->
                themeMode = mode
                themePreferences.edit { putInt("theme_mode", mode.ordinal) }
            },
            onRefresh = viewModel::refresh,
            onRefreshLocal = viewModel::refreshLocalApks,
            onEnterLocal = { requestLocalApkAccess() },
            onDeleteLocal = viewModel::deleteLocalApk,
            onRenameLocal = viewModel::renameLocalApk,
            onExport = requestExport,
            onCustomExport = requestCustomExport,
            onShare = requestShare,
            onCustomShare = requestCustomShare,
            onBatchShare = requestBatchShare,
            onBatchExport = requestBatchExport,
            onCheckUpdate = AppUpdateManager::manualCheck,
            onHelp = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://daxiaamu.github.io/APKExport/help/index.html".toUri())) },
            onBatchInstaller = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://optool.daxiaamu.com/super_adb".toUri())) },
            onOriginalProject = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/leftshine/APKExport".toUri())) },
            onMaintainerProject = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/daxiaamu/APKExport".toUri())) },
        )
        if (batchExportRunning) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.modern_batch_export)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.modern_batch_export_progress, batchExportCompleted, batchExportTotal))
                        LinearProgressIndicator(
                            progress = { if (batchExportTotal == 0) 0f else batchExportCompleted.toFloat() / batchExportTotal },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {},
            )
        }
        UpdateDialogHost()
    }
}

private suspend fun exportAndReport(context: Context, viewModel: MainViewModel, app: InstalledApp, snackbar: SnackbarHostState, requestedFileName: String? = null) {
    viewModel.export(app, requestedFileName).onSuccess { snackbar.showSnackbar(context.getString(R.string.modern_exported_to, it.displayPath)) }
        .onFailure { snackbar.showSnackbar(context.getString(R.string.modern_export_failed, it.message ?: context.getString(R.string.modern_unknown_error))) }
}

internal fun shareApk(context: Context, exported: ExportedApk) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = ApkRepository.APK_MIME
        putExtra(Intent.EXTRA_STREAM, exported.uri)
        clipData = ClipData.newUri(context.contentResolver, "APK", exported.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, context.getString(cn.leftshine.apkexport.R.string.share_to, "APK")))
}

private fun shareApks(context: Context, exported: List<ExportedApk>) {
    if (exported.size == 1) {
        shareApk(context, exported.first())
        return
    }
    val uris = ArrayList(exported.map { it.uri })
    val sharedClipData = ClipData.newUri(context.contentResolver, "APK", uris.first())
    uris.drop(1).forEach { sharedClipData.addItem(ClipData.Item(it)) }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = ApkRepository.APK_MIME
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        clipData = sharedClipData
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, context.getString(cn.leftshine.apkexport.R.string.share_to, "APK")))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    state: MainUiState,
    snackbar: SnackbarHostState,
    initialDestination: MainDestination,
    darkMode: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRefresh: () -> Unit,
    onRefreshLocal: () -> Unit,
    onEnterLocal: () -> Unit,
    onDeleteLocal: (LocalApkFile) -> Unit,
    onRenameLocal: (LocalApkFile, String) -> Unit,
    onExport: (InstalledApp) -> Unit,
    onCustomExport: (InstalledApp, String) -> Unit,
    onShare: (InstalledApp) -> Unit,
    onCustomShare: (InstalledApp, String) -> Unit,
    onBatchShare: (List<InstalledApp>) -> Unit,
    onBatchExport: (List<InstalledApp>) -> Unit,
    onCheckUpdate: () -> Unit,
    onHelp: () -> Unit,
    onBatchInstaller: () -> Unit,
    onOriginalProject: () -> Unit,
    onMaintainerProject: () -> Unit,
) {
    val context = LocalContext.current
    val filterPreferences = remember { context.getSharedPreferences("app_filters", Context.MODE_PRIVATE) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var destination by remember(initialDestination) { mutableStateOf(initialDestination) }
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val collapsibleContentHeight = if (destination == MainDestination.EXPORT) 96.dp else 48.dp
    SideEffect {
        val heightLimit = -with(density) { collapsibleContentHeight.toPx() }
        topAppBarScrollBehavior.state.heightOffsetLimit = heightLimit
        topAppBarScrollBehavior.state.heightOffset = topAppBarScrollBehavior.state.heightOffset.coerceIn(heightLimit, 0f)
    }
    val destinations = MainDestination.entries
    val pagerState = rememberPagerState(initialPage = destination.ordinal, pageCount = { destinations.size })
    val pagerScope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) {
        destination = destinations[pagerState.currentPage]
        topAppBarScrollBehavior.state.heightOffset = 0f
        topAppBarScrollBehavior.state.contentOffset = 0f
        if (destination == MainDestination.LOCAL_APK) onEnterLocal()
    }
    val exportListState = rememberLazyGridState()
    val localListState = rememberLazyGridState()
    var query by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var sortType by rememberSaveable { mutableStateOf(AppSort.entries.getOrElse(filterPreferences.getInt("sort_type", 0)) { AppSort.NAME }) }
    var sortAscending by rememberSaveable { mutableStateOf(filterPreferences.getBoolean("sort_ascending", true)) }
    var multiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var showUserApps by rememberSaveable { mutableStateOf(filterPreferences.getBoolean("show_user_apps", true)) }
    var showSystemApps by rememberSaveable { mutableStateOf(filterPreferences.getBoolean("show_system_apps", false)) }
    BackHandler {
        when {
            multiSelectMode -> {
                multiSelectMode = false
                selectedPackages = emptySet()
            }
            searchFocused || query.isNotEmpty() -> {
                query = ""
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
            else -> (context as? android.app.Activity)?.finish()
        }
    }
    val userAppCount = remember(state.apps) { state.apps.count { !it.isSystem } }
    val systemAppCount = remember(state.apps) { state.apps.count { it.isSystem } }
    val visibleApps = remember(state.apps, query, showUserApps, showSystemApps, sortType, sortAscending) {
        val filtered = state.apps.filter { app ->
            ((!app.isSystem && showUserApps) || (app.isSystem && showSystemApps)) &&
                (query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true))
        }
        val comparator = when (sortType) {
            AppSort.NAME -> compareBy<InstalledApp> { it.label.lowercase(Locale.getDefault()) }
            AppSort.PACKAGE -> compareBy { it.packageName.lowercase(Locale.ROOT) }
            AppSort.SIZE -> compareBy { it.size }
            AppSort.INSTALLED -> compareBy { it.firstInstallTime }
            AppSort.UPDATED -> compareBy { it.lastUpdateTime }
        }
        filtered.sortedWith(if (sortAscending) comparator else comparator.reversed())
    }
    val selectedApps = remember(visibleApps, selectedPackages) { visibleApps.filter { it.packageName in selectedPackages } }

    if (showSortDialog) {
        val labels = listOf(R.string.modern_sort_name, R.string.modern_sort_package, R.string.modern_sort_size, R.string.modern_sort_installed, R.string.modern_sort_updated).map { stringResource(it) }
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.modern_sort)) },
            text = { Column {
                AppSort.entries.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            sortType = option
                            filterPreferences.edit { putInt("sort_type", index) }
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sortType == option, onClick = null)
                        Text(labels[index])
                    }
                }
            } },
            confirmButton = { TextButton(onClick = {
                sortAscending = false
                filterPreferences.edit { putBoolean("sort_ascending", false) }
                showSortDialog = false
            }) { Text(stringResource(R.string.modern_descending)) } },
            dismissButton = { TextButton(onClick = {
                sortAscending = true
                filterPreferences.edit { putBoolean("sort_ascending", true) }
                showSortDialog = false
            }) { Text(stringResource(R.string.modern_ascending)) } },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection).pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        },
        topBar = {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            Spacer(Modifier.height(statusBarHeight))
            Box(
                modifier = Modifier.fillMaxWidth().height(collapsibleContentHeight * (1f - topAppBarScrollBehavior.state.collapsedFraction)).clipToBounds(),
                contentAlignment = Alignment.BottomCenter,
            ) {
            Column(Modifier.fillMaxWidth().requiredHeight(collapsibleContentHeight)) {
            TopAppBar(
                modifier = Modifier.requiredHeight(48.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(when (destination) { MainDestination.EXPORT -> "APKExport"; MainDestination.LOCAL_APK -> stringResource(R.string.modern_tab_local_apk); MainDestination.SETTINGS -> stringResource(R.string.modern_tab_settings) }, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                },
                actions = {
                    if (destination == MainDestination.EXPORT && multiSelectMode) {
                        IconButton(onClick = {
                            selectedPackages = if (selectedPackages.size == visibleApps.size) emptySet() else visibleApps.mapTo(mutableSetOf()) { it.packageName }
                        }) { Icon(Icons.Outlined.DoneAll, stringResource(R.string.modern_select_all)) }
                        IconButton(onClick = { onBatchShare(selectedApps) }, enabled = selectedApps.isNotEmpty()) { Icon(Icons.Outlined.Share, stringResource(R.string.modern_batch_share)) }
                        IconButton(onClick = { onBatchExport(selectedApps) }, enabled = selectedApps.isNotEmpty()) { Icon(Icons.Outlined.Archive, stringResource(R.string.modern_batch_export)) }
                        IconButton(onClick = { multiSelectMode = false; selectedPackages = emptySet() }) { Icon(Icons.Outlined.Close, stringResource(R.string.modern_exit_multi_select)) }
                    } else if (destination == MainDestination.EXPORT) {
                        IconButton(onClick = { showSortDialog = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, stringResource(R.string.modern_sort)) }
                        IconButton(onClick = { multiSelectMode = true }) { Icon(Icons.Outlined.Checklist, stringResource(R.string.modern_multi_select)) }
                        IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, stringResource(R.string.modern_refresh)) }
                    }
                    if (destination == MainDestination.LOCAL_APK) IconButton(onClick = onRefreshLocal, enabled = !state.localApksLoading) { Icon(Icons.Outlined.Refresh, stringResource(R.string.modern_refresh)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
            if (destination == MainDestination.EXPORT) {
                Row(
                    modifier = Modifier.fillMaxWidth().requiredHeight(48.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).height(48.dp).onFocusChanged { searchFocused = it.isFocused },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) Text(stringResource(R.string.modern_search_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        innerTextField()
                                    }
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { query = "" }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.modern_clear_search), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        },
                    )
                    AnimatedVisibility(visible = searchFocused) {
                        TextButton(
                            onClick = {
                                query = ""
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            },
                        ) { Text(stringResource(R.string.modern_cancel)) }
                    }
                }
                }
            }
            }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == MainDestination.EXPORT,
                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(MainDestination.EXPORT.ordinal) } },
                    icon = { Icon(Icons.Outlined.Archive, null) },
                    label = { Text(stringResource(R.string.modern_tab_export)) },
                )
                NavigationBarItem(
                    selected = destination == MainDestination.LOCAL_APK,
                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(MainDestination.LOCAL_APK.ordinal) } },
                    icon = { Icon(Icons.Outlined.FolderOpen, null) },
                    label = { Text(stringResource(R.string.modern_tab_local_apk)) },
                )
                NavigationBarItem(
                    selected = destination == MainDestination.SETTINGS,
                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(MainDestination.SETTINGS.ordinal) } },
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text(stringResource(R.string.modern_tab_settings)) },
                )
            }
        },
    ) { contentPadding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (destinations[page]) {
                MainDestination.EXPORT -> {
            Column(Modifier.fillMaxSize().padding(contentPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(selected = showUserApps, onClick = {
                        if (!showUserApps || showSystemApps) {
                            showUserApps = !showUserApps
                            filterPreferences.edit { putBoolean("show_user_apps", showUserApps) }
                        }
                    }, label = { Text(stringResource(R.string.modern_user_apps_count, userAppCount)) })
                    FilterChip(selected = showSystemApps, onClick = {
                        if (!showSystemApps || showUserApps) {
                            showSystemApps = !showSystemApps
                            filterPreferences.edit { putBoolean("show_system_apps", showSystemApps) }
                        }
                    }, label = { Text(stringResource(R.string.modern_system_apps_count, systemAppCount)) })
                }
                AnimatedVisibility(visible = state.loading) {
                    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
                if (!state.loading && visibleApps.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (query.isBlank()) stringResource(R.string.modern_no_apps) else stringResource(R.string.modern_no_search_results, query), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(320.dp),
                        state = exportListState,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppArchiveCard(
                                app = app,
                                multiSelectMode = multiSelectMode,
                                selected = app.packageName in selectedPackages,
                                onToggleSelection = { selectedPackages = if (app.packageName in selectedPackages) selectedPackages - app.packageName else selectedPackages + app.packageName },
                                onExport = { onExport(app) },
                                onCustomExport = { name -> onCustomExport(app, name) },
                                onShare = { onShare(app) },
                                onCustomShare = { name -> onCustomShare(app, name) },
                            )
                        }
                    }
                }
            }
                }
                MainDestination.LOCAL_APK -> {
            LocalApkScreen(files = state.localApks, loading = state.localApksLoading, listState = localListState, modifier = Modifier.fillMaxSize().padding(contentPadding), onInstall = { installLocalApk(context, it) }, onShare = { shareLocalApk(context, it) }, onDelete = onDeleteLocal, onRename = onRenameLocal)
                }
                MainDestination.SETTINGS -> {
                    SettingsScreen(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                darkMode = darkMode,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onCheckUpdate = onCheckUpdate,
                onHelp = onHelp,
                onBatchInstaller = onBatchInstaller,
                onOriginalProject = onOriginalProject,
                onMaintainerProject = onMaintainerProject,
                    )
                }
            }
        }
    }
}

private fun localPackageMime(name: String): String = if (name.endsWith(".apk", true)) ApkRepository.APK_MIME else "application/zip"

private fun installLocalApk(context: Context, file: LocalApkFile) = LocalPackageInstaller.install(context, file)

private fun shareLocalApk(context: Context, file: LocalApkFile) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = localPackageMime(file.name)
        putExtra(Intent.EXTRA_STREAM, file.uri)
        clipData = ClipData.newUri(context.contentResolver, file.name, file.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, context.getString(R.string.modern_share_apk)))
}

@Composable
private fun LocalApkScreen(
    files: List<LocalApkFile>,
    loading: Boolean,
    listState: androidx.compose.foundation.lazy.grid.LazyGridState,
    modifier: Modifier,
    onInstall: (LocalApkFile) -> Unit,
    onShare: (LocalApkFile) -> Unit,
    onDelete: (LocalApkFile) -> Unit,
    onRename: (LocalApkFile, String) -> Unit,
) {
    val context = LocalContext.current
    var actionFile by remember { mutableStateOf<LocalApkFile?>(null) }
    var renameFile by remember { mutableStateOf<LocalApkFile?>(null) }
    var renameText by remember { mutableStateOf("") }

    actionFile?.let { file ->
        AlertDialog(
            onDismissRequest = { actionFile = null },
            title = { Text(file.name, style = MaterialTheme.typography.titleMedium) },
            text = { Column {
                Text(file.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { actionFile = null; onShare(file) }) { Text(stringResource(R.string.modern_share), Modifier.fillMaxWidth()) }
                TextButton(onClick = { actionFile = null; onInstall(file) }) { Text(stringResource(R.string.modern_install), Modifier.fillMaxWidth()) }
                TextButton(onClick = {
                    actionFile = null
                    renameFile = file
                    renameText = file.name.substringBeforeLast('.', file.name)
                }) { Text(stringResource(R.string.modern_rename), Modifier.fillMaxWidth()) }
                TextButton(onClick = { actionFile = null; onDelete(file) }) { Text(stringResource(R.string.modern_delete), Modifier.fillMaxWidth()) }
            } },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionFile = null }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }
    renameFile?.let { file ->
        AlertDialog(
            onDismissRequest = { renameFile = null },
            title = { Text(stringResource(R.string.modern_rename)) },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, suffix = { Text(".${file.name.substringAfterLast('.', "apk")}") }) },
            confirmButton = { TextButton(onClick = { onRename(file, renameText); renameFile = null }, enabled = renameText.isNotBlank()) { Text(stringResource(R.string.modern_confirm)) } },
            dismissButton = { TextButton(onClick = { renameFile = null }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }

    Column(modifier) {
        AnimatedVisibility(visible = loading) {
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
        if (!loading && files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.modern_no_local_apks), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(320.dp), state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp)) {
        items(files, key = { it.uri.toString() }) { file ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).clickable { actionFile = file },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val packageIcon by produceState<Bitmap?>(initialValue = null, file.uri, file.modifiedAt) {
                        value = LocalPackageIconLoader.load(context, file)
                    }
                    Box(Modifier.padding(start = 4.dp).size(36.dp), contentAlignment = Alignment.Center) {
                        packageIcon?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                            ?: Icon(Icons.Outlined.Archive, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${formatBytes(file.size)} · ${formatModifiedAt(file.modifiedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                HorizontalDivider(Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
            }
        }
    }
}
}
@Composable
private fun SettingsScreen(
    modifier: Modifier,
    darkMode: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onHelp: () -> Unit,
    onBatchInstaller: () -> Unit,
    onOriginalProject: () -> Unit,
    onMaintainerProject: () -> Unit,
) {
    var showFilenameDialog by remember { mutableStateOf(false) }
    var filenameFormat by remember { mutableStateOf(Settings.getCustomFileNameFormat().orEmpty()) }
    var exportPath by remember { mutableStateOf(Settings.getCustomExportPath().orEmpty()) }

    val context = LocalContext.current
    val chooseExportDirectory = stringResource(R.string.modern_choose_export_directory)
    val exportFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        documentTreePath(uri)?.let { path -> Settings.setCustomExportPath(path); exportPath = path }
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeLabels = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.modern_theme_system),
        ThemeMode.LIGHT to stringResource(R.string.modern_theme_light),
        ThemeMode.DARK to stringResource(R.string.modern_theme_dark),
    )
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.modern_dark_mode), style = MaterialTheme.typography.titleMedium) },
            text = { Column {
                themeLabels.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onThemeModeChange(mode); showThemeDialog = false }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = themeMode == mode, onClick = null)
                        Text(label)
                    }
                }
            } },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }
    if (showFilenameDialog) {
        AlertDialog(
            onDismissRequest = { showFilenameDialog = false },
            title = { Text(stringResource(R.string.modern_custom_filename_format), style = MaterialTheme.typography.titleMedium) },
            text = { Column {
                OutlinedTextField(value = filenameFormat, onValueChange = { filenameFormat = it }, singleLine = true)
                Text(stringResource(R.string.modern_filename_format_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.modern_filename_placeholder_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } },
            confirmButton = { TextButton(onClick = { Settings.setCustomFileNameFormat(filenameFormat.ifBlank { "#N-#P-#V" }); filenameFormat = Settings.getCustomFileNameFormat(); showFilenameDialog = false }) { Text(stringResource(R.string.modern_confirm)) } },
            dismissButton = { TextButton(onClick = { showFilenameDialog = false }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }
    Column(modifier.verticalScroll(rememberScrollState())) {
        SettingsGroup(stringResource(R.string.modern_group_general)) {
            SettingsItem(
                title = stringResource(R.string.modern_dark_mode),
                icon = if (darkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                onClick = { showThemeDialog = true },
                trailingContent = {
                    Text(
                        themeLabels.first { it.first == themeMode }.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            SettingsItem(
                title = stringResource(R.string.modern_custom_filename_format),
                summary = filenameFormat,
                icon = Icons.Outlined.Info,
                onClick = {
                    filenameFormat = Settings.getCustomFileNameFormat().orEmpty()
                    showFilenameDialog = true
                },
            )
            SettingsItem(
                title = stringResource(R.string.modern_export_location),
                summary = exportPath,
                icon = Icons.Outlined.Archive,
                onClick = {
                    Toast.makeText(context.applicationContext, chooseExportDirectory, Toast.LENGTH_LONG).show()
                    exportFolderLauncher.launch(null)
                },
            )
        }
        SettingsSectionDivider()
        SettingsGroup(stringResource(R.string.modern_group_update)) {
            SettingsItem(
                title = stringResource(R.string.modern_check_update),
                summary = stringResource(R.string.modern_current_version, BuildConfig.VERSION_NAME),
                icon = Icons.Outlined.SystemUpdate,
                trailingContent = { UpdateAction(onCheckUpdate) },
            )
        }
        SettingsSectionDivider()
        SettingsGroup(stringResource(R.string.modern_group_help)) {
            SettingsExternalLinkItem(
                title = stringResource(R.string.modern_help),
                summary = stringResource(R.string.modern_help_summary),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = onHelp,
            )
            SettingsExternalLinkItem(
                title = stringResource(R.string.modern_batch_installer),
                summary = stringResource(R.string.modern_batch_installer_summary),
                icon = Icons.Outlined.InstallMobile,
                onClick = onBatchInstaller,
            )
        }
        SettingsSectionDivider()
        SettingsGroup(stringResource(R.string.modern_group_about)) {
            SettingsExternalLinkItem(
                title = stringResource(R.string.modern_maintainer),
                summary = stringResource(R.string.modern_maintainer_name),
                icon = Icons.Outlined.Info,
                onClick = onMaintainerProject,
            )
            SettingsExternalLinkItem(
                title = stringResource(R.string.modern_original_project),
                summary = "GitHub · APKExport",
                icon = Icons.Outlined.Info,
                onClick = onOriginalProject,
            )
        }
    }
}

private fun documentTreePath(uri: Uri): String? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
    val separator = documentId.indexOf(':')
    val volume = if (separator >= 0) documentId.substring(0, separator) else documentId
    val relativePath = if (separator >= 0) documentId.substring(separator + 1) else ""
    val root = if (volume.equals("primary", ignoreCase = true)) Environment.getExternalStorageDirectory() else File("/storage", volume)
    return File(root, relativePath).absolutePath
}
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp),
        )
        content()
    }
}
@Composable
private fun SettingsItem(
    title: String,
    icon: ImageVector,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    var itemModifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
    if (onClick != null) itemModifier = itemModifier.clickable(onClick = onClick)
    ListItem(
        modifier = itemModifier,
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = summary?.let { value ->
            {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = { Icon(icon, null) },
        trailingContent = trailingContent,
    )
}

@Composable
private fun SettingsExternalLinkItem(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SettingsItem(
        title = title,
        summary = summary,
        icon = icon,
        onClick = onClick,
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
    )
}

@Composable
private fun SettingsSectionDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppArchiveCard(
    app: InstalledApp,
    multiSelectMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onExport: () -> Unit,
    onCustomExport: (String) -> Unit,
    onShare: () -> Unit,
    onCustomShare: (String) -> Unit,
) {
    val context = LocalContext.current
    val copyAppName = stringResource(R.string.modern_copy_app_name)
    val copyPackage = stringResource(R.string.modern_copy_package)
    val copyVersion = stringResource(R.string.modern_copy_version)
    val copyVersionCode = stringResource(R.string.modern_copy_version_code)
    val windowHeightDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val customEditorMaxLines = ((windowHeightDp.value * 0.45f) / 24f).toInt().coerceAtLeast(4)
    val icon by produceState<Bitmap?>(initialValue = null, app.packageName) { value = AppIconLoader.load(context, app.packageName) }
    var showCopyMenu by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showCustomNameDialog by remember { mutableStateOf(false) }
    var customNameForShare by remember { mutableStateOf(false) }
    var customFileName by remember { mutableStateOf("") }
    fun generatedFileName(): String {
        val format = Settings.getCustomFileNameFormat().orEmpty().ifBlank { "#N-#P-#V" }
        return format.replace("#N", app.label).replace("#P", app.packageName).replace("#V", app.versionName).replace("#C", app.versionCode.toString()) + ".apk"
    }
    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        showCopyMenu = false
        Toast.makeText(context, R.string.modern_copied, Toast.LENGTH_SHORT).show()
    }
    if (showCopyMenu) {
        AlertDialog(
            onDismissRequest = { showCopyMenu = false },
            title = { Text(app.label, style = MaterialTheme.typography.titleMedium) },
            text = { Column {
                TextButton(onClick = { copy(copyAppName, app.label) }) { Text(stringResource(R.string.modern_copy_app_name), Modifier.fillMaxWidth()) }
                TextButton(onClick = { copy(copyPackage, app.packageName) }) { Text(stringResource(R.string.modern_copy_package), Modifier.fillMaxWidth()) }
                TextButton(onClick = { copy(copyVersion, app.versionName) }) { Text(stringResource(R.string.modern_copy_version), Modifier.fillMaxWidth()) }
                TextButton(onClick = { copy(copyVersionCode, app.versionCode.toString()) }) { Text(stringResource(R.string.modern_copy_version_code), Modifier.fillMaxWidth()) }
            } },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCopyMenu = false }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }
    if (showCustomNameDialog) {
        AlertDialog(
            onDismissRequest = { showCustomNameDialog = false },
            title = { Text(stringResource(R.string.input_new_name), style = MaterialTheme.typography.titleMedium) },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = Color.Transparent,
                ) {
                    Box(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)) {
                        BasicTextField(
                            value = customFileName,
                            onValueChange = { customFileName = it },
                            modifier = Modifier.fillMaxWidth().padding(end = 28.dp),
                            singleLine = false,
                            minLines = 1,
                            maxLines = customEditorMaxLines,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        )
                        if (customFileName.isNotEmpty()) {
                            Icon(
                                Icons.Outlined.Close,
                                stringResource(R.string.modern_clear_text),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clickable { customFileName = "" },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                val name = customFileName.trim()
                showCustomNameDialog = false
                if (customNameForShare) onCustomShare(name) else onCustomExport(name)
            }, enabled = customFileName.isNotBlank()) { Text(stringResource(R.string.modern_confirm)) } },
            dismissButton = { TextButton(onClick = { showCustomNameDialog = false }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }
    if (showActionMenu) {
        AlertDialog(
            onDismissRequest = { showActionMenu = false },
            title = { Text(stringResource(R.string.modern_choose_action), style = MaterialTheme.typography.titleMedium) },
            text = { Column {

                TextButton(onClick = { showActionMenu = false; onExport() }) { Text(stringResource(R.string.modern_export), Modifier.fillMaxWidth()) }
                TextButton(onClick = { showActionMenu = false; customFileName = generatedFileName(); customNameForShare = false; showCustomNameDialog = true }) { Text(stringResource(R.string.modern_custom_export), Modifier.fillMaxWidth()) }
                TextButton(onClick = { showActionMenu = false; onShare() }) { Text(stringResource(R.string.modern_share), Modifier.fillMaxWidth()) }
                TextButton(onClick = { showActionMenu = false; customFileName = generatedFileName(); customNameForShare = true; showCustomNameDialog = true }) { Text(stringResource(R.string.modern_custom_share), Modifier.fillMaxWidth()) }
            } },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showActionMenu = false }) { Text(stringResource(R.string.modern_cancel)) } },
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (multiSelectMode) onToggleSelection() else showActionMenu = true }, onLongClick = { if (multiSelectMode) onToggleSelection() else showCopyMenu = true }),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(start = 4.dp).size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                icon?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
            }
            Column(Modifier.weight(1f).padding(start = 10.dp, end = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(app.label, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val tagBackground = if (app.isSystem) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                    val tagForeground = if (app.isSystem) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    Surface(color = tagBackground, contentColor = tagForeground, shape = RoundedCornerShape(4.dp)) {
                        Text(if (app.isSystem) stringResource(R.string.modern_system_tag) else stringResource(R.string.modern_user_tag), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp)
                    }
                    }
                    Text("v${app.versionName}", modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.packageName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatBytes(app.size), modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (multiSelectMode) {
                Checkbox(selected, onCheckedChange = { onToggleSelection() }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 46.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}
private fun formatModifiedAt(epochSeconds: Long): String = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(epochSeconds * 1000L))
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}