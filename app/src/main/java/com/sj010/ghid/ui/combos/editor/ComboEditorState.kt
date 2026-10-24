package com.sj010.ghid.ui.combos.editor

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.ComboEmojiPolicy
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.parser.SequenceAnalysis
import com.sj010.ghid.domain.parser.SequenceAnalyzer

/**
 * Owns the editable fields of a combo and derives everything the UI needs from the
 * single scheme string. No composable parses sequences on its own.
 */
class ComboEditorState(
    private val initial: Combo?,
    initialScheme: String = "",
    defaultOs: TargetOs = TargetOs.WINDOWS
) {
    var name by mutableStateOf(initial?.name.orEmpty())
    var os by mutableStateOf(if (initial != null) TargetOs.fromComboTag(initial.os) else defaultOs)
    var emoji by mutableStateOf(initial?.emoji.orEmpty())
    var description by mutableStateOf(initial?.description.orEmpty())
    var scheme by mutableStateOf(initial?.keys ?: initialScheme)

    val analysis: SequenceAnalysis by derivedStateOf { SequenceAnalyzer.analyze(scheme, os) }
    val emojiValid: Boolean by derivedStateOf { ComboEmojiPolicy.isValid(emoji) }
    val canSave: Boolean by derivedStateOf { name.isNotBlank() && analysis.canSend && emojiValid }

    fun build(): Combo = Combo(
        name = name.trim(),
        os = os.token,
        emoji = ComboEmojiPolicy.normalize(emoji),
        description = description.trim(),
        keys = scheme.trim(),
        favorite = initial?.favorite ?: false,
        createdAt = initial?.createdAt.orEmpty()
    )
}
