package com.sj010.ghid.ui.snippets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.Snippet
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.theme.ExpressiveShapes

/**
 * The full Material 3 Expressive snippet screen inside the morphing notification shade.
 *
 * Uses official MaterialShapes catalog for expressive leading icons,
 * CircleShape (shape.full) for buttons per M3E spec, tonal surface hierarchy,
 * and spring-based motion for interactive feedback.
 */
enum class SnippetTypeFilter(val label: String) {
    ALL("All"),
    TYPE("Type"),
    RAW("Raw")
}

@Composable
fun SnippetShadeContent(
    service: HidService?,
    showFilter: Boolean,
    filterQuery: String,
    onQueryChange: (String) -> Unit,
    selectedTypeFilter: SnippetTypeFilter = SnippetTypeFilter.ALL,
    onTypeFilterChange: (SnippetTypeFilter) -> Unit = {},
    onCloseFilter: () -> Unit
) {
    val repo = GhostHidApp.instance.payloadRepository
    val snippets by repo.snippets.collectAsState()

    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var editorOpen by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    val typeCount = remember(snippets) { snippets.count { !it.isRaw } }
    val rawCount = remember(snippets) { snippets.count { it.isRaw } }

    val filteredSnippets = remember(snippets, filterQuery, selectedTypeFilter) {
        snippets.filter { snippet ->
            val matchesType = when (selectedTypeFilter) {
                SnippetTypeFilter.ALL -> true
                SnippetTypeFilter.TYPE -> !snippet.isRaw
                SnippetTypeFilter.RAW -> snippet.isRaw
            }
            if (!matchesType) return@filter false

            if (filterQuery.isBlank()) return@filter true

            val q = filterQuery.trim().lowercase()
            snippet.name.lowercase().contains(q) ||
                (snippet.value?.lowercase()?.contains(q) == true) ||
                snippet.effectiveType.lowercase().contains(q)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // TOP: NEW SNIPPET ACTION (simple, bold, light tonal fill)
            // ==========================================
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingSnippet = null
                    editorOpen = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                shape = CircleShape, // M3E: buttons use shape.full
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New payload",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // FILTER / SEARCH BAR (EXPANDABLE)
            // ==========================================
            AnimatedVisibility(
                visible = showFilter,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact M3E search pill (height: 40.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (filterQuery.isEmpty()) {
                                    Text(
                                        text = "Filter payloads...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        maxLines = 1
                                    )
                                }
                                BasicTextField(
                                    value = filterQuery,
                                    onValueChange = onQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                                )
                            }
                            if (filterQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    onCloseFilter()
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close filter",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Type filter chips: All, Type, Raw
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SnippetFilterChip(
                            label = "All (${snippets.size})",
                            selected = selectedTypeFilter == SnippetTypeFilter.ALL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTypeFilterChange(SnippetTypeFilter.ALL)
                            }
                        )

                        SnippetFilterChip(
                            label = "Type ($typeCount)",
                            selected = selectedTypeFilter == SnippetTypeFilter.TYPE,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTypeFilter == SnippetTypeFilter.TYPE) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTypeFilterChange(SnippetTypeFilter.TYPE)
                            }
                        )

                        SnippetFilterChip(
                            label = "Raw ($rawCount)",
                            selected = selectedTypeFilter == SnippetTypeFilter.RAW,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTypeFilter == SnippetTypeFilter.RAW) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTypeFilterChange(SnippetTypeFilter.RAW)
                            }
                        )
                    }
                }
            }

            // ==========================================
            // SNIPPET CARDS LIST (MATERIAL 3 EXPRESSIVE)
            // ==========================================
            if (snippets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge, // M3E: prominent cards use extraLarge
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(ExpressiveShapes.Clover4Leaf) // Official M3E shape
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Snippets Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create reusable payloads for typing credentials, commands, and raw HID reports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (filteredSnippets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (filterQuery.isNotBlank()) {
                                "No payloads match \"$filterQuery\""
                            } else {
                                "No ${selectedTypeFilter.label.lowercase()} payloads found"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTypeFilter != SnippetTypeFilter.ALL || filterQuery.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    onQueryChange("")
                                    onTypeFilterChange(SnippetTypeFilter.ALL)
                                }
                            ) {
                                Text("Clear filter")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(filteredSnippets, key = { _, s -> s.name }) { index, snippet ->
                        M3ExpressiveSnippetCard(
                            snippet = snippet,
                            index = index,
                            onType = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val textToType = snippet.value ?: ""
                                if (textToType.isNotEmpty()) {
                                    if (snippet.isRaw) {
                                        val cleanHex = (if (textToType.startsWith("raw:", ignoreCase = true)) textToType.substring(4) else textToType)
                                            .replace(" ", "")
                                        if (cleanHex.length in listOf(2, 4, 8, 16) && cleanHex.all { it in "0123456789abcdefABCDEF" }) {
                                            val bytes = ByteArray(cleanHex.length / 2) { i ->
                                                cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                                            }
                                            service?.sendRawReport(bytes)
                                        }
                                    } else {
                                        service?.typeText(textToType)
                                    }
                                }
                            },
                            onCopy = {
                                val valToCopy = snippet.value ?: ""
                                clipboardManager.setText(AnnotatedString(valToCopy))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onEdit = {
                                editingSnippet = snippet
                                editorOpen = true
                            },
                            onDelete = {
                                repo.delete(snippet.name)
                            }
                        )
                    }

                    // Spacer at bottom so content is never obscured by the bottom dock
                    item {
                        Spacer(modifier = Modifier.height(76.dp))
                    }
                }
            }
        }

    }

    if (editorOpen) {
        SnippetEditorPage(
            initial = editingSnippet,
            onDismiss = { editorOpen = false },
            onSave = { name, value, secure, type ->
                repo.set(name, value, secure, type)
                editorOpen = false
            }
        )
    }
}

// =========================================================================
// MATERIAL 3 EXPRESSIVE SNIPPET CARD
//
// A single tonal card per snippet: expressive shape badge, identity + type
// chip, one monospace value surface, and a primary "Type to PC" action with
// a secondary copy button. Edit/Delete live in an overflow menu so the card
// keeps one clear focal action and everything stays at a 48dp touch target.
// =========================================================================

/** Tonal pairing for the expressive leading badge, matching M3 container roles. */
private data class SnippetAccent(val container: Color, val content: Color)

// ==========================================
// MATERIAL 3 EXPRESSIVE FILTER CHIP
// ==========================================

@Composable
private fun SnippetFilterChip(
    label: String,
    selected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = icon,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** Curated M3E shapes for snippet card leading icons, chosen for visual variety. */
private val snippetLeadingShapes: List<Shape> = listOf(
    ExpressiveShapes.Cookie4Sided,
    ExpressiveShapes.Flower,
    ExpressiveShapes.Arch,
    ExpressiveShapes.Diamond,
    ExpressiveShapes.Sunny,
    ExpressiveShapes.Ghostish,
    ExpressiveShapes.Cookie9Sided,
    ExpressiveShapes.SoftBurst
)

@Composable
fun M3ExpressiveSnippetCard(
    snippet: Snippet,
    index: Int,
    onType: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // Dynamic M3 Expressive shape from official catalog per card index
    val badgeShape: Shape = snippetLeadingShapes[index % snippetLeadingShapes.size]

    // Tonal accent rotation using all three M3 container roles + surface highest
    val accent = when (index % 4) {
        0 -> SnippetAccent(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        1 -> SnippetAccent(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        2 -> SnippetAccent(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> SnippetAccent(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.primary
        )
    }

    val hasValue = !snippet.value.isNullOrEmpty()
    val maskedValue = if (hasValue) "••••••••••••" else "(empty secret)"
    val plainValue = if (hasValue) snippet.value.orEmpty() else "(empty value)"
    val displayValue = if (snippet.secure && !revealed) maskedValue else plainValue

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp), // M3E: prominent cards use shape.extraLarge
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: expressive badge + identity + overflow actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(badgeShape)
                        .background(accent.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (snippet.secure) Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        tint = accent.content,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "{${snippet.name}}",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (snippet.isRaw) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (snippet.isRaw) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = if (snippet.isRaw) "RAW" else "TEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = if (snippet.secure) Icons.Default.Security else Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (snippet.secure) "Keystore enclave" else "Plain text",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        },
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Payload actions",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Edit payload",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Monospace value surface
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayValue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = when {
                            !hasValue -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            snippet.secure && !revealed -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (snippet.secure) {
                        IconButton(
                            onClick = { revealed = !revealed },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (revealed) "Hide secret" else "Reveal secret",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Primary action + secondary copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onType()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape, // M3E: buttons use shape.full
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    Icon(
                        imageVector = if (snippet.isRaw) Icons.AutoMirrored.Filled.Send else Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = if (snippet.isRaw) "Send" else "Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCopy()
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy payload",
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// MATERIAL 3 EXPRESSIVE SNIPPET EDITOR PAGE
//
// A full-screen dialog page in the same shape language as the combo editor:
// expressive header, scrollable form, and a commit bar pinned above the
// keyboard so Cancel / Save are always reachable while typing.
// =========================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SnippetEditorPage(
    initial: Snippet?,
    onDismiss: () -> Unit,
    onSave: (name: String, value: String, secure: Boolean, type: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var value by remember { mutableStateOf(initial?.value ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "type") }
    var secure by remember { mutableStateOf(initial?.secure ?: false) }
    var valueVisible by remember { mutableStateOf(initial != null && !initial.secure) }

    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val canSave = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // -----------------------------------------------------------
                // Header
                // -----------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel and close"
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initial == null) "New payload" else "Edit payload",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (initial == null) {
                                "Store a string, then type it on your PC."
                            } else {
                                "Update what gets typed on your PC."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(ExpressiveShapes.Clover4Leaf)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // -----------------------------------------------------------
                // Scrollable form
                // -----------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Selection between Type (text) and Raw (HID report)
                    Text(
                        text = "PAYLOAD TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.6.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isText = type.equals("type", ignoreCase = true)
                        val isRaw = type.equals("raw", ignoreCase = true)

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                type = "type"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = CircleShape,
                            color = if (isText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = if (isText) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Type (Text)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                type = "raw"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = CircleShape,
                            color = if (isRaw) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isRaw) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = if (isRaw) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Send (Raw HID)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Payload name") },
                        placeholder = { Text("e.g. username or my_password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        prefix = {
                            Text("{", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        },
                        suffix = {
                            Text("}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        },
                        singleLine = true,
                        enabled = initial == null,
                        supportingText = {
                            Text(
                                text = if (initial == null) {
                                    "Reference it in combos as {name}"
                                } else {
                                    "Names are fixed once created"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    val isRawSelected = type.equals("raw", ignoreCase = true)
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(if (isRawSelected) "Raw HID Bytes (Hex)" else "Value (typed on PC)") },
                        placeholder = {
                            Text(
                                if (isRawSelected) "e.g. 02 or cd00" else "Enter string to type",
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = if (isRawSelected) 2 else 3,
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        visualTransformation = if (secure && !valueVisible) {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        } else {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        },
                        trailingIcon = {
                            if (secure) {
                                IconButton(onClick = { valueVisible = !valueVisible }) {
                                    Icon(
                                        imageVector = if (valueVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (valueVisible) "Hide value" else "Show value"
                                    )
                                }
                            }
                        },
                        supportingText = {
                            Text(
                                text = if (isRawSelected) "Dispatched directly as raw USB/BLE HID report bytes" else "${value.length} characters · sent exactly as written",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    // Paste Material Three expressive button below the form field
                    FilledTonalButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                value = clip
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from clipboard",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paste from Clipboard",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Keystore toggle container
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hardware keystore enclave",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Encrypt secret with AES-256 in secure hardware",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = secure,
                                onCheckedChange = { secure = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // -----------------------------------------------------------
                // Pinned commit bar
                // -----------------------------------------------------------
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSave(name.trim(), value, secure, type)
                            },
                            enabled = canSave,
                            modifier = Modifier
                                .weight(1.35f)
                                .height(58.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = if (initial == null) "Save payload" else "Update",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
