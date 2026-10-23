package com.poc.hiddebug.ui.snippets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.poc.hiddebug.GhostHidApp
import com.poc.hiddebug.domain.model.Snippet
import com.poc.hiddebug.service.HidService
import com.poc.hiddebug.ui.theme.MaterialShapes
import kotlinx.coroutines.delay

/**
 * The full Material 3 Expressive snippet screen inside the morphing notification shade.
 *
 * Uses official MaterialShapes catalog for expressive leading icons,
 * CircleShape (shape.full) for buttons per M3E spec, tonal surface hierarchy,
 * and spring-based motion for interactive feedback.
 */
@Composable
fun SnippetShadeContent(
    service: HidService?,
    showFilter: Boolean,
    filterQuery: String,
    onQueryChange: (String) -> Unit,
    onCloseFilter: () -> Unit
) {
    val repo = GhostHidApp.instance.snippetRepository
    val snippets by repo.snippets.collectAsState()

    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var statusToast by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(statusToast) {
        if (statusToast != null) {
            delay(2200)
            statusToast = null
        }
    }

    val filteredSnippets = remember(snippets, filterQuery) {
        if (filterQuery.isBlank()) {
            snippets
        } else {
            snippets.filter { snippet ->
                snippet.name.contains(filterQuery, ignoreCase = true) ||
                    (!snippet.value.isNullOrEmpty() && snippet.value.contains(filterQuery, ignoreCase = true))
            }
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
            // TOP: NEW SNIPPET PILL BUTTON (M3E shape.full)
            // ==========================================
            Button(
                onClick = {
                    editingSnippet = null
                    editorOpen = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = CircleShape, // M3E: buttons use shape.full
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New snippet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    placeholder = {
                        Text(
                            text = "Filter snippets by name or value...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filterQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                onCloseFilter()
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close filter",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large, // M3E: search fields use shape.large
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                )
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
                                    .clip(MaterialShapes.ShapeClover4) // Official M3E shape
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
                                text = "Create quick-typing snippets for usernames, passwords, emails, and commands.",
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
                    Text(
                        text = "No snippets match \"$filterQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                    service?.typeText(textToType)
                                    statusToast = "⚡ Typed $${snippet.name} to PC"
                                } else {
                                    statusToast = "⚠️ Snippet $${snippet.name} is empty"
                                }
                            },
                            onCopy = {
                                val valToCopy = snippet.value ?: ""
                                clipboardManager.setText(AnnotatedString(valToCopy))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                statusToast = "📋 Copied $${snippet.name} to clipboard"
                            },
                            onEdit = {
                                editingSnippet = snippet
                                editorOpen = true
                            },
                            onDelete = {
                                repo.delete(snippet.name)
                                statusToast = "🗑️ Deleted $${snippet.name}"
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

        // ==========================================
        // FLOATING ACTION TOAST BANNER (M3E inverseSurface pill)
        // ==========================================
        AnimatedVisibility(
            visible = statusToast != null,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = statusToast ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }

    if (editorOpen) {
        ExpressiveSnippetEditorDialog(
            initial = editingSnippet,
            onDismiss = { editorOpen = false },
            onSave = { name, value, secure ->
                repo.set(name, value, secure)
                statusToast = if (editingSnippet == null) "✅ Added \$$name" else "✅ Updated \$$name"
                editorOpen = false
            }
        )
    }
}

// =========================================================================
// MATERIAL 3 EXPRESSIVE SNIPPET CARD
//
// Uses official MaterialShapes catalog for per-card expressive leading icons,
// CircleShape (shape.full) for action buttons, proper tonal hierarchy,
// and FilledTonalIconButton for M3-compliant icon actions.
// =========================================================================

/** Curated M3E shapes for snippet card leading icons, chosen for visual variety. */
private val snippetLeadingShapes: List<Shape> = listOf(
    MaterialShapes.ShapeClover4,
    MaterialShapes.ShapeFlower,
    MaterialShapes.ShapeDiamond,
    MaterialShapes.ShapeCookie9,
    MaterialShapes.ShapeSunny,
    MaterialShapes.ShapeGhostish,
    MaterialShapes.ShapeHeart,
    MaterialShapes.ShapeSoftBurst
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

    // Dynamic M3 Expressive shape from official catalog per card index
    val shape: Shape = snippetLeadingShapes[index % snippetLeadingShapes.size]

    // Tonal accent rotation using all three M3 container roles + surface highest
    val (containerColor, contentColor) = when (index % 4) {
        0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.primary
    }

    // Animated border color for visual feedback
    val borderColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "snippetBorder"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge, // M3E: 28dp extraLarge for prominent cards
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Leading Expressive Shape + Name/Keystore Badge + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expressive leading icon shape from official M3E catalog
                Box(
                    modifier = Modifier
                        .size(44.dp) // M3E: 44dp for prominent leading visuals
                        .clip(shape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (snippet.secure) Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$${snippet.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (snippet.secure) {
                        // M3E: Chip-like badge with CircleShape
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Keystore Enclave",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "Plain text",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Action buttons: M3E FilledTonalIconButton for Copy/Edit, error-toned for Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy snippet",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit snippet",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete snippet",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Monospace Value Preview Box
            Surface(
                shape = MaterialTheme.shapes.large, // M3E: shape.large (16dp) for inner containers
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayValue = if (snippet.secure && !revealed) {
                        if (!snippet.value.isNullOrEmpty()) "••••••••••••" else "(empty secret)"
                    } else {
                        if (!snippet.value.isNullOrEmpty()) snippet.value else "(empty value)"
                    }

                    Text(
                        text = displayValue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = if (snippet.value.isNullOrEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (snippet.secure) {
                        IconButton(onClick = { revealed = !revealed }, modifier = Modifier.size(28.dp)) {
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

            // Prominent Full-Width "Type to PC" Action Button (M3E: shape.full for buttons)
            Button(
                onClick = onType,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape, // M3E: buttons use shape.full
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Type \"$${snippet.name}\" to PC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// =========================================================================
// MATERIAL 3 EXPRESSIVE SNIPPET EDITOR DIALOG
//
// Uses M3E shape tokens throughout: extraLarge for dialog, CircleShape for
// buttons and preset chips, shape.medium for text fields, shape.large for
// the toggle container. Official MaterialShapes for decorative elements.
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressiveSnippetEditorDialog(
    initial: Snippet?,
    onDismiss: () -> Unit,
    onSave: (name: String, value: String, secure: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var value by remember { mutableStateOf(initial?.value ?: "") }
    var secure by remember { mutableStateOf(initial?.secure ?: false) }
    var valueVisible by remember { mutableStateOf(!secure) }

    val presetNames = listOf("username", "password", "email", "sudo_pass", "token", "wifi_key")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge, // M3E: dialogs use shape.extraLarge
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialShapes.ShapeClover4) // Official M3E shape
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (initial == null) "New Snippet" else "Edit Snippet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initial == null) {
                    Text(
                        text = "QUICK PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.6.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetNames.forEach { preset ->
                            // M3E: Chip-like preset buttons use CircleShape (shape.full)
                            Surface(
                                shape = CircleShape,
                                color = if (name.equals(preset, ignoreCase = true)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                onClick = {
                                    name = preset
                                    if (preset.contains("pass") || preset.contains("token") || preset.contains("key")) {
                                        secure = true
                                    }
                                }
                            ) {
                                Text(
                                    text = "$$preset",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (name.equals(preset, ignoreCase = true)) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Variable Name") },
                    placeholder = { Text("e.g. username or my_password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium, // M3E: text fields use shape.medium (12dp)
                    prefix = { Text("$", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                    enabled = initial == null
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value (Typed on PC)") },
                    placeholder = { Text("Enter string to type") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium, // M3E: text fields use shape.medium (12dp)
                    trailingIcon = {
                        IconButton(onClick = { valueVisible = !valueVisible }) {
                            Icon(
                                imageVector = if (valueVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (valueVisible) "Hide" else "Show"
                            )
                        }
                    }
                )

                // Keystore toggle container
                Surface(
                    shape = MaterialTheme.shapes.large, // M3E: shape.large for containers
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hardware Keystore Enclave",
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), value, secure) },
                enabled = name.isNotBlank(),
                shape = CircleShape, // M3E: buttons use shape.full
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape // M3E: buttons use shape.full
            ) {
                Text("Cancel")
            }
        }
    )
}
