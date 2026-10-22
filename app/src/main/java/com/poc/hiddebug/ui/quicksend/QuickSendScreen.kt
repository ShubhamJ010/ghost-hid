package com.poc.hiddebug.ui.quicksend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.poc.hiddebug.service.HidService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSendScreen(service: HidService?) {
    var textInput by remember { mutableStateOf("") }

    // Modifier matrix state
    var modWin by remember { mutableStateOf(false) }
    var modCtrl by remember { mutableStateOf(false) }
    var modAlt by remember { mutableStateOf(false) }
    var modShift by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Direct Typing Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instant Text Injection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type any text or command to send...") },
                        trailingIcon = {
                            if (textInput.isNotEmpty()) {
                                IconButton(onClick = { textInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${textInput.length} chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                if (textInput.isNotEmpty()) {
                                    service?.typeText(textInput)
                                }
                            },
                            enabled = textInput.isNotEmpty(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Type on Host")
                        }
                    }
                }
            }
        }

        // Chord Builder with Modifier Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Modifier Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = modWin,
                            onClick = { modWin = !modWin },
                            label = { Text("Win") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = modCtrl,
                            onClick = { modCtrl = !modCtrl },
                            label = { Text("Ctrl") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = modAlt,
                            onClick = { modAlt = !modAlt },
                            label = { Text("Alt") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = modShift,
                            onClick = { modShift = !modShift },
                            label = { Text("Shift") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Combine with key:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Enter", "Esc", "Tab", "Space", "Backspace", "Del", "F1", "F2", "F5", "Up", "Down", "Left", "Right").forEach { keyName ->
                            SuggestionChip(
                                onClick = {
                                    val mods = mutableListOf<String>()
                                    if (modWin) mods.add("win")
                                    if (modCtrl) mods.add("ctrl")
                                    if (modAlt) mods.add("alt")
                                    if (modShift) mods.add("shift")
                                    mods.add(keyName.lowercase())
                                    service?.executeCombo(mods.joinToString("+"))
                                },
                                label = { Text(keyName) }
                            )
                        }
                    }
                }
            }
        }

        // Common Chords Quick Grid
        item {
            Text(
                text = "Preset Chords",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            val commonChords = listOf(
                "Win + D" to "win+d",
                "Alt + Tab" to "alt+tab",
                "Ctrl + Shift + Esc" to "ctrl+shift+esc",
                "Win + R" to "win+r",
                "Win + X" to "win+x",
                "Ctrl + C" to "ctrl+c",
                "Ctrl + V" to "ctrl+v",
                "Ctrl + Z" to "ctrl+z",
                "Ctrl + A" to "ctrl+a"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonChords.forEach { (label, chord) ->
                    OutlinedButton(
                        onClick = { service?.executeCombo(chord) },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
