package com.sj010.ghid.ui.combos.editor

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.parser.SequenceAnalysis
import com.sj010.ghid.domain.parser.SequenceAnalyzer

/**
 * Owns the editable fields of a combo and derives everything the UI needs from the
 * single scheme string. No composable parses sequences on its own.
 */
class ComboEditorState(
    private val initial: Combo?,
    initialScheme: String = ""
) {
    var name by mutableStateOf(initial?.name.orEmpty())
    var os by mutableStateOf(TargetOs.fromComboTag(initial?.os))
    var description by mutableStateOf(initial?.description.orEmpty())
    var scheme by mutableStateOf(initial?.keys ?: initialScheme)

    val analysis: SequenceAnalysis by derivedStateOf { SequenceAnalyzer.analyze(scheme, os) }
    val canSave: Boolean by derivedStateOf { name.isNotBlank() && analysis.canSend }

    fun build(): Combo = Combo(
        name = name.trim(),
        os = os.token,
        description = description.trim(),
        keys = scheme.trim(),
        favorite = initial?.favorite ?: false,
        createdAt = initial?.createdAt.orEmpty()
    )
}
