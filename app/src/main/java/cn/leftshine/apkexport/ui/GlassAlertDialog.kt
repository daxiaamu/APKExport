package cn.leftshine.apkexport.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalGlassHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val hazeState = LocalGlassHazeState.current
    val darkSurface = colors.surface.luminance() < 0.5f
    val glassTintAlpha = if (darkSurface) 0.68f else 0.60f
    val glassStyle = HazeStyle(
        backgroundColor = colors.surfaceContainerHigh,
        tint = HazeTint(colors.surfaceContainerHigh.copy(alpha = glassTintAlpha)),
        blurRadius = 26.dp,
        noiseFactor = 0f,
    )
    val glassModifier = if (hazeState != null) {
        modifier
            .clip(shape)
            .hazeEffect(hazeState, style = glassStyle)
            .border(
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.46f)),
                shape = shape,
            )
    } else {
        modifier.border(
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.46f)),
            shape = shape,
        )
    }

    MaterialAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = glassModifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = if (hazeState != null) Color.Transparent else colors.surfaceContainerHigh,
        iconContentColor = colors.primary,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties,
    )
}

