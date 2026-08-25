package cn.leftshine.apkexport.update

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import cn.leftshine.apkexport.R
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.leftshine.apkexport.BuildConfig

@Composable
internal fun UpdateAction(onManualCheck: () -> Unit) {
    val state by AppUpdateManager.state.collectAsStateWithLifecycle()
    Box(Modifier.size(64.dp, 48.dp), contentAlignment = Alignment.Center) {
        if (state.checkStatus == CheckStatus.CHECKING) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onManualCheck, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.modern_update_action), style = MaterialTheme.typography.bodyMedium) }
            if (state.hasUpdateDot) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp)
                        .size(8.dp),
                ) { androidx.compose.foundation.Canvas(Modifier.size(8.dp)) { drawCircle(Color(0xFFBA1A1A)) } }
            }
        }
    }
}

@Composable
internal fun UpdateDialogHost() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val state by AppUpdateManager.state.collectAsStateWithLifecycle()
    val manifest = state.manifest ?: return
    if (!state.showDialog) return
    val forced = manifest.isForced(BuildConfig.VERSION_CODE.toLong())
    val downloading = state.downloadStatus == DownloadStatus.DOWNLOADING || state.downloadStatus == DownloadStatus.VERIFYING
    val ready = state.downloadStatus == DownloadStatus.READY || state.downloadStatus == DownloadStatus.NEEDS_AUTHORIZATION
    val progress = state.progress
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.60f

    AlertDialog(
        onDismissRequest = { if (!forced && !downloading) AppUpdateManager.ignore() },
        title = { Text(stringResource(if (forced) R.string.update_required else R.string.update_available)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.update_version, manifest.versionName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                manifest.localPublishedAt()?.let { Text(stringResource(R.string.update_published, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                SelectionContainer { SimpleMarkdown(manifest.changelog.ifBlank { stringResource(R.string.update_no_changelog) }) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        dismissButton = {
            if (!forced) {
                Row {
                    TextButton(onClick = AppUpdateManager::skip, enabled = !downloading) { Text(stringResource(R.string.update_skip_version)) }
                    TextButton(onClick = AppUpdateManager::ignore, enabled = !downloading) { Text(stringResource(R.string.update_ignore)) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (ready) AppUpdateManager.install(activity) else AppUpdateManager.download() },
                enabled = !downloading && state.downloadStatus != DownloadStatus.LAUNCHING,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                when {
                    downloading -> {
                        if (progress != null) CircularProgressIndicator({ progress / 100f }, Modifier.size(20.dp), strokeWidth = 2.dp)
                        else CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                        Text(progress?.let { stringResource(R.string.update_downloading_progress, it) } ?: stringResource(R.string.update_downloading))
                    }
                    ready -> Text(stringResource(R.string.modern_install))
                    state.downloadStatus == DownloadStatus.FAILED -> Text(stringResource(R.string.update_retry))
                    else -> Text(stringResource(R.string.update_download_install))
                }
            }
        },
    )
}

@Composable
private fun SimpleMarkdown(markdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var inCodeBlock = false
        markdown.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trimStart().startsWith("```")) {
                inCodeBlock = !inCodeBlock
            } else if (line.isBlank()) {
                Spacer(Modifier.size(2.dp))
            } else {
                val (prefix, content, style) = when {
                    inCodeBlock -> Triple("", line, MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    line.startsWith("### ") -> Triple("", line.removePrefix("### "), MaterialTheme.typography.titleSmall)
                    line.startsWith("## ") -> Triple("", line.removePrefix("## "), MaterialTheme.typography.titleMedium)
                    line.startsWith("# ") -> Triple("", line.removePrefix("# "), MaterialTheme.typography.titleLarge)
                    line.matches(Regex("[-*_]{3,}")) -> Triple("", "────────", MaterialTheme.typography.bodySmall)
                    line.startsWith("> ") -> Triple("▎ ", line.removePrefix("> "), MaterialTheme.typography.bodyMedium)
                    line.matches(Regex("^\\d+\\. .*")) -> Triple("", line, MaterialTheme.typography.bodyMedium)
                    line.startsWith("- ") || line.startsWith("* ") -> Triple("• ", line.drop(2), MaterialTheme.typography.bodyMedium)
                    else -> Triple("", line, MaterialTheme.typography.bodyMedium)
                }
                Text(prefix + inlineMarkdown(content), style = style)
            }
        }
    }
}

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    val token = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`|https?://[^\\s)]+)")
    token.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val value = match.value
        when {
            value.startsWith("**") -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold)).also { append(value.drop(2).dropLast(2)); pop() }
            value.startsWith("`") -> pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1A808080))).also { append(value.drop(1).dropLast(1)); pop() }
            else -> pushStyle(SpanStyle(textDecoration = TextDecoration.Underline)).also { append(value); pop() }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}