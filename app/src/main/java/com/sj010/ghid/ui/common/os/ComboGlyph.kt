package com.sj010.ghid.ui.common.os

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.ComboEmojiPolicy

/**
 * The single combo icon: the combo's emoji metadata when set, otherwise the
 * target OS logo. Because the emoji lives on the combo, it overrides the OS
 * glyph everywhere the combo's identity is shown.
 */
@Composable
fun ComboGlyph(
    combo: Combo,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    val emoji = ComboEmojiPolicy.normalize(combo.emoji)
    if (emoji == null) {
        OsGlyph(
            os = combo.targetOs,
            modifier = modifier.size(size),
            tint = tint,
            contentDescription = contentDescription
        )
        return
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            text = emoji,
            fontSize = (size.value * 0.86f).sp,
            color = tint
        )
    }
}
