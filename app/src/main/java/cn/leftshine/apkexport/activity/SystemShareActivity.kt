package cn.leftshine.apkexport.activity

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cn.leftshine.apkexport.R
import cn.leftshine.apkexport.utils.Settings
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SystemShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sourcePackage = referrer?.takeIf { it.scheme == "android-app" }?.host
            ?: callingPackage
            ?: packageName
        setContent { SystemShareScreen(sourcePackage) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SystemShareScreen(sourcePackage: String) {
        val context = LocalContext.current
        val resources = LocalResources.current
        val scope = rememberCoroutineScope()
        var app by remember { mutableStateOf<InstalledApp?>(null) }
        var fileName by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(true) }
        var exporting by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var pendingShare by remember { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) app?.let { export(it, fileName, pendingShare, { exporting = it }, { message = it }) }
            else message = resources.getString(R.string.modern_legacy_storage_required)
        }

        fun requestExport(share: Boolean) {
            val current = app ?: return
            pendingShare = share
            if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else export(current, fileName, share, { exporting = it }, { message = it })
        }

        LaunchedEffect(sourcePackage) {
            app = withContext(Dispatchers.IO) { loadInstalledApp(sourcePackage) }
            fileName = app?.let(::defaultFileName).orEmpty()
            loading = false
            if (app == null) message = resources.getString(R.string.modern_app_info_failed)
        }

        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = { IconButton(onClick = ::finish) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.modern_cancel)) } },
                        actions = {
                            IconButton(onClick = { openMain(ModernMainActivity.DESTINATION_EXPORT) }) { Icon(Icons.Outlined.Home, stringResource(R.string.action_main)) }
                            IconButton(onClick = { openMain(ModernMainActivity.DESTINATION_SETTINGS) }) { Icon(Icons.Outlined.Settings, stringResource(R.string.action_settings)) }
                        },
                    )
                },
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        app != null -> {
                            val current = checkNotNull(app)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val icon = remember(current.packageName) { packageManager.getApplicationIcon(current.packageName).toBitmapSafe().asImageBitmap() }
                                Image(icon, null, Modifier.size(52.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(current.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(current.packageName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${current.versionName} · ${formatShareBytes(current.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            OutlinedTextField(
                                value = fileName,
                                onValueChange = { fileName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.modern_custom_filename_format)) },
                                enabled = !exporting,
                                maxLines = 4,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { requestExport(false) }, enabled = !exporting && fileName.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.modern_export)) }
                                Button(onClick = { requestExport(true) }, enabled = !exporting && fileName.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.modern_export_and_share)) }
                            }
                            if (exporting) CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                        }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    private fun export(app: InstalledApp, requestedName: String, share: Boolean, setExporting: (Boolean) -> Unit, report: (String) -> Unit) {
        setExporting(true)
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { ApkRepository(applicationContext).export(app, requestedName.trim()) } }
                .onSuccess {
                    if (share) shareApk(this@SystemShareActivity, it)
                    report(getString(R.string.modern_exported_to, it.displayPath))
                }
                .onFailure { report(getString(R.string.modern_export_failed, it.message ?: getString(R.string.modern_unknown_error))) }
            setExporting(false)
        }
    }

    @Suppress("DEPRECATION")
    private fun loadInstalledApp(packageName: String): InstalledApp? = runCatching {
        val info = if (Build.VERSION.SDK_INT >= 33) packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)) else packageManager.getPackageInfo(packageName, 0)
        val applicationInfo = checkNotNull(info.applicationInfo)
        InstalledApp(
            label = applicationInfo.loadLabel(packageManager).toString(),
            packageName = info.packageName,
            versionName = info.versionName.orEmpty().ifBlank { "—" },
            versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
            sourcePath = applicationInfo.sourceDir,
            size = File(applicationInfo.sourceDir).length(),
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
        )
    }.getOrNull()

    private fun defaultFileName(app: InstalledApp): String = Settings.getCustomFileNameFormat().orEmpty().ifBlank { "#N-#P-#V" }
        .replace("#N", app.label).replace("#P", app.packageName).replace("#V", app.versionName).replace("#C", app.versionCode.toString()) + ".apk"

    private fun openMain(destination: Int) = startActivity(Intent(this, ModernMainActivity::class.java).putExtra(ModernMainActivity.EXTRA_DESTINATION, destination))

    private fun formatShareBytes(bytes: Long): String = if (bytes >= 1024 * 1024) String.format(Locale.getDefault(), "%.1f MB", bytes / 1024.0 / 1024.0) else String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
}