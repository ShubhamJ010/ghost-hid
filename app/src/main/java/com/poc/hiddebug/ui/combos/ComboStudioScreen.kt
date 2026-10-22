package com.poc.hiddebug.ui.combos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poc.hiddebug.GhostHidApp
import com.poc.hiddebug.domain.model.Combo
import com.poc.hiddebug.domain.model.HidStep
import com.poc.hiddebug.domain.parser.ActionSequenceParser
import com.poc.hiddebug.service.HidService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComboStudioScreen(service: HidService?) {
    val repo = GhostHidApp.instance.comboRepository
    val combos by repo.combos.collectAsState()

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingCombo by remember { mutableStateOf<Combo?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCombo = null
                    showEditorSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Combo")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sort: favorites first, then alphabetically
            val sorted = combos.sortedWith(compareByDescending<Combo> { it.favorite }.thenBy { it.name })

            items(sorted, key = { it.name }) { combo ->
                ComboCard(
                    combo = combo,
                    onRun = { service?.executeCombo(combo.name) },
                    onToggleFavorite = { repo.toggleFavorite(combo.name) },
                    onEdit = {
                        editingCombo = combo
                        showEditorSheet = true
                    },
                    onDelete = { repo.delete(combo.name) }
                )
            }
        }
    }

    if (showEditorSheet) {
        ComboEditorBottomSheet(
            initial = editingCombo,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showEditorSheet = false }
            },
            onSave = { saved ->
                repo.addOrUpdate(saved)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showEditorSheet = false }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComboCard(
    combo: Combo,
    onRun: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = combo.emoji ?: "⚡",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = combo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!combo.description.isNullOrEmpty()) {
                            Text(
                                text = combo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (combo.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (combo.favorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AST Step Pills
            val steps = remember(combo.keys) { ActionSequenceParser.parse(combo.keys) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                steps.forEach { step ->
                    StepBadge(step = step)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Button(
                    onClick = onRun,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Macro", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StepBadge(step: HidStep) {
    val (label, bg, fg) = when (step) {
        is HidStep.KeyboardChord -> Triple(step.label, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        is HidStep.TypeText -> Triple("\"${step.text}\"", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        is HidStep.Variable -> Triple("$${step.name}", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        is HidStep.Delay -> Triple("⏱ ${(step.millis / 1000.0)}s", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
        is HidStep.MouseAction -> Triple("🖱 ${step.action}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        is HidStep.Directive -> Triple("!${step.name}", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboEditorBottomSheet(
    initial: Combo?,
    onDismiss: () -> Unit,
    onSave: (Combo) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "⚡") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var keys by remember { mutableStateOf(initial?.keys ?: "") }

    val validationResult = remember(keys) { ActionSequenceParser.validate(keys) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initial == null) "New Combo Macro" else "Edit Combo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji") },
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Combo Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = keys,
                onValueChange = { keys = it },
                label = { Text("Keys Grammar Sequence") },
                placeholder = { Text("win+r ->{0.5} type:cmd ->{0.3} enter") },
                modifier = Modifier.fillMaxWidth(),
                isError = validationResult.isFailure && keys.isNotEmpty(),
                supportingText = {
                    if (validationResult.isFailure && keys.isNotEmpty()) {
                        Text(
                            text = validationResult.exceptionOrNull()?.message ?: "Invalid syntax",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Use '->{sec}' for delays, 'type:<text>' for strings, '\$VAR' for variables")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            Combo(
                                name = name.trim(),
                                emoji = emoji.trim(),
                                description = description.trim(),
                                keys = keys.trim(),
                                favorite = initial?.favorite ?: false,
                                createdAt = initial?.createdAt ?: ""
                            )
                        )
                    },
                    enabled = name.isNotBlank() && validationResult.isSuccess,
                    shape = CircleShape
                ) {
                    Text("Save Combo")
                }
            }
        }
    }
}
