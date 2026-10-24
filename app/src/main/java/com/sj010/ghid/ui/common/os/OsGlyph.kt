package com.sj010.ghid.ui.common.os

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.sj010.ghid.R
import com.sj010.ghid.domain.model.TargetOs

/**
 * Host OS logo used as the only combo icon in the app.
 *
 * The vectors are single-path silhouettes, so every caller tints them with a
 * Material 3 color role instead of a brand color. UNKNOWN keeps the generic
 * laptop glyph.
 */
@Composable
fun OsGlyph(
    os: TargetOs,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    when (os) {
        TargetOs.WINDOWS -> Icon(
            painter = painterResource(R.drawable.ic_os_windows),
            contentDescription = contentDescription ?: "Windows",
            tint = tint,
            modifier = modifier
        )
        TargetOs.MACOS -> Icon(
            painter = painterResource(R.drawable.ic_os_apple),
            contentDescription = contentDescription ?: "macOS",
            tint = tint,
            modifier = modifier
        )
        TargetOs.LINUX -> Icon(
            painter = painterResource(R.drawable.ic_os_linux),
            contentDescription = contentDescription ?: "Linux",
            tint = tint,
            modifier = modifier
        )
        TargetOs.UNKNOWN -> Icon(
            imageVector = Icons.Outlined.Laptop,
            contentDescription = contentDescription ?: "Unknown host",
            tint = tint,
            modifier = modifier
        )
    }
}
