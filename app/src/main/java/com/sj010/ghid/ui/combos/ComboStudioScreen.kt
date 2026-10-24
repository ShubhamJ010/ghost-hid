package com.sj010.ghid.ui.combos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.Snippet
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.parser.SequenceAnalyzer
import com.sj010.ghid.ui.common.os.ComboGlyph
import com.sj010.ghid.ui.common.os.OsGlyph
import com.sj010.ghid.ui.combos.editor.ComboEditorScreen
import com.sj010.ghid.ui.common.sequence.SequencePreview
import com.sj010.ghid.ui.common.sequence.SequencePreviewDialog
import com.sj010.ghid.ui.common.sequence.formatSequenceDuration
import com.sj010.ghid.ui.snippets.SnippetsShade
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.theme.MaterialShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ComboFilterCategory(val label: String, val os: TargetOs? = null) {
    ALL("All"),
    FAVORITES("Favorites"),
    WINDOWS("Windows", TargetOs.WINDOWS),
    MACOS("macOS", TargetOs.MACOS),
    LINUX("Linux", TargetOs.LINUX);

    companion object {
        /** The chip selected when a host OS is fingerprinted. */
        fun forOs(os: TargetOs): ComboFilterCategory =
            entries.firstOrNull { it.os == os && os.isKnown } ?: ALL
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComboStudioScreen(
    service: HidService?
) {
    val repo = GhostHidApp.instance.comboRepository
    val combos by repo.combos.collectAsState()

    val targetOs by service?.targetOs?.collectAsState()
        ?: remember { mutableStateOf(HostOsFingerprint.Unknown) }
    val connectedOs = targetOs.os

    var searchQuery by remember { mutableStateOf("") }
    // The connected host selects the default view until the user taps a chip.
    var filterChosen by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ComboFilterCategory.ALL) }

    LaunchedEffect(connectedOs) {
        if (!filterChosen) {
            selectedFilter = ComboFilterCategory.forOs(connectedOs)
        }
    }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingCombo by remember { mutableStateOf<Combo?>(null) }
    var comboToDelete by remember { mutableStateOf<Combo?>(null) }

    var executingComboName by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Filter and search logic
    val filteredCombos = remember(combos, searchQuery, selectedFilter) {
        combos.filter { combo ->
            // Search query matches name, description, keys, or target OS name
            val matchesQuery = searchQuery.isBlank() ||
                combo.name.contains(searchQuery, ignoreCase = true) ||
                (combo.description?.contains(searchQuery, ignoreCase = true) == true) ||
                combo.targetOs.label.contains(searchQuery, ignoreCase = true) ||
                combo.keys.contains(searchQuery, ignoreCase = true)

            if (!matchesQuery) return@filter false

            when (val os = selectedFilter.os) {
                null -> selectedFilter == ComboFilterCategory.ALL || combo.favorite
                else -> combo.targetOs == os
            }
        }.sortedWith(
            compareByDescending<Combo> { it.favorite }
                .thenBy { it.name.lowercase() }
        )
    }

    // Category counts for quick tabs
    val favoriteCount = remember(combos) { combos.count { it.favorite } }
    val osCounts = remember(combos) {
        TargetOs.entries.filter { it.isKnown }.associateWith { os -> combos.count { it.targetOs == os } }
    }

    SnippetsShade(service = service) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingCombo = null
                    showEditorSheet = true
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = "New Combo",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp // Clear bottom FAB and navigation bar
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {


                // ==========================================
                // SECTION 2: SEARCH & FILTER CHIPS ROW
                // ==========================================
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search text field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),							  placeholder = {
                                Text(
                                    "Search combos, keys, or OS...",
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
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        )

                        // Filter chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ComboFilterChip(
                                label = "All (${combos.size})",
                                selected = selectedFilter == ComboFilterCategory.ALL,
                                onClick = {
                                    filterChosen = true
                                    selectedFilter = ComboFilterCategory.ALL
                                }
                            )

                            ComboFilterChip(
                                label = "★ Favs ($favoriteCount)",
                                selected = selectedFilter == ComboFilterCategory.FAVORITES,
                                onClick = {
                                    filterChosen = true
                                    selectedFilter = ComboFilterCategory.FAVORITES
                                }
                            )

                            ComboFilterCategory.entries
                                .filter { it.os != null }
                                .forEach { category ->
                                    val os = category.os ?: TargetOs.UNKNOWN
                                    ComboFilterChip(
                                        label = "${category.label} (${osCounts[os] ?: 0})",
                                        selected = selectedFilter == category,
                                        icon = {
                                            OsGlyph(
                                                os = os,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (selectedFilter == category) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        },
                                        onClick = {
                                            filterChosen = true
                                            selectedFilter = category
                                        }
                                    )
                                }
                        }
                    }
                }

                // ==========================================
                // SECTION 3: COMBO CARDS LIST OR EMPTY STATE
                // ==========================================
                if (filteredCombos.isEmpty()) {
                    item {
                        EmptyCombosState(
                            hasSearchQuery = searchQuery.isNotBlank(),
                            osFilter = selectedFilter.os,
                            onClearFilters = {
                                searchQuery = ""
                                filterChosen = true
                                selectedFilter = ComboFilterCategory.ALL
                            },
                            onCreateNew = {
                                editingCombo = null
                                showEditorSheet = true
                            }
                        )
                    }
                } else {
                    items(
                        items = filteredCombos,
                        key = { it.name }
                    ) { combo ->
                        val isExecuting = executingComboName == combo.name

                        ComboCard(
                            combo = combo,
                            isExecuting = isExecuting,
                            onRun = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                executingComboName = combo.name
                                service?.executeCombo(combo.keys)

                                scope.launch {
                                    delay(1200)
                                    if (executingComboName == combo.name) {
                                        executingComboName = null
                                    }
                                }
                            },
                            onToggleFavorite = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                repo.toggleFavorite(combo.name)
                            },
                            onEdit = {
                                editingCombo = combo
                                showEditorSheet = true
                            },
                            onDelete = {
                                comboToDelete = combo
                            }
                        )
                    }
                }
            }

        }
    }
    }

    // ==========================================
    // DIALOG: CONFIRM DELETE
    // ==========================================
    if (comboToDelete != null) {
        val target = comboToDelete!!
        AlertDialog(
            onDismissRequest = { comboToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete \"${target.name}\"?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "This will permanently remove this combo sequence and any scheduled macros. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repo.delete(target.name)
                        comboToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = CircleShape
                ) {
                    Text("Delete Combo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { comboToDelete = null },
                    shape = CircleShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // BOTTOM SHEET: COMBO EDITOR & TESTER
    // ==========================================
    if (showEditorSheet) {
        ComboEditorScreen(
            initial = editingCombo,
            service = service,
            onDismiss = {
                showEditorSheet = false
            },
            onSave = { saved ->
                repo.addOrUpdate(saved)
                showEditorSheet = false
            }
        )
    }
}

// =========================================================================
// FILTER CHIP COMPONENT
// =========================================================================

@Composable
private fun ComboFilterChip(
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

// =========================================================================
// COMBO CARD (EXPRESSIVE, DYNAMIC, MATERIAL 3)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComboCard(
    combo: Combo,
    isExecuting: Boolean,
    onRun: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Bouncy heart scale animation
    val favScale by animateFloatAsState(
        targetValue = if (combo.favorite) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "favScale"
    )

    val analysis = remember(combo.keys, combo.os) { SequenceAnalyzer.analyze(combo.keys, combo.targetOs) }

    // Expressive shape mapping per combo name to give visual variety
    val expressiveShape = remember(combo.name) {
        val hash = kotlin.math.abs(combo.name.hashCode())
        val shapes = listOf(
            MaterialShapes.ShapeClover4,
            MaterialShapes.ShapeFlower,
            MaterialShapes.ShapeArch,
            MaterialShapes.ShapeDiamond,
            MaterialShapes.ShapeSunny,
            MaterialShapes.ShapeCookie9,
            MaterialShapes.ShapeGhostish,
            MaterialShapes.ShapeSoftBurst
        )
        shapes[hash % shapes.size]
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            1.dp,
            if (isExecuting) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Expressive OS-logo shape + title + subtitle + favorite heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Expressive shape badge carrying the target OS logo
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(expressiveShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ComboGlyph(
                            combo = combo,
                            size = 24.dp,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = combo.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                        }

                        if (!combo.description.isNullOrEmpty()) {
                            Text(
                                text = combo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.scale(favScale)
                ) {
                    Icon(
                        imageVector = if (combo.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (combo.favorite) "Unstar" else "Star",
                        tint = if (combo.favorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Preview body only: the duration pill is tilted onto its top edge and the
            // full breakdown lives behind a long press.
            Box(modifier = Modifier.fillMaxWidth()) {
                SequencePreview(
                    analysis = analysis,
                    compact = true,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    showHeader = false,
                    showStatusPill = false,
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showPreviewDialog = true
                    }
                )

                if (analysis.steps.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = (-13).dp)
                            .rotate(-7f)
                    ) {
                        Text(
                            text = formatSequenceDuration(analysis.totalDelayMillis),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: Edit + Delete on Left, "Run Combo" Button on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Run Combo Button with spring feedback
                Button(
                    onClick = onRun,
                    shape = CircleShape,
                    interactionSource = interactionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExecuting) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        contentColor = if (isExecuting) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sending...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Run Combo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showPreviewDialog) {
        SequencePreviewDialog(
            analysis = analysis,
            onDismiss = { showPreviewDialog = false },
            onEdit = onEdit
        )
    }
}

// =========================================================================
// EMPTY STATE COMPONENT
// =========================================================================

@Composable
private fun EmptyCombosState(
    hasSearchQuery: Boolean,
    osFilter: TargetOs? = null,
    onClearFilters: () -> Unit,
    onCreateNew: () -> Unit
) {
    val title = when {
        osFilter != null -> "No ${osFilter.label} combos yet"
        hasSearchQuery -> "No matching combos"
        else -> "No combos found"
    }
    val body = when {
        osFilter != null -> "The connected ${osFilter.label} host has no combos yet. Show all combos or create one for it."
        hasSearchQuery -> "Try clearing search filters to see all combos."
        else -> "Tap below to create your first automated keystroke sequence."
    }
    val showClear = osFilter != null || hasSearchQuery
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialShapes.ShapeFlower)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasSearchQuery) Icons.Default.FilterList else Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (showClear) {
            Button(
                onClick = onClearFilters,
                shape = CircleShape
            ) {
                Text(if (osFilter != null) "Show all combos" else "Clear Filters")
            }
        } else {
            Button(
                onClick = onCreateNew,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create First Combo")
            }
        }
    }
}

// =========================================================================
// TOP CARD: SNIPPETS VAULT
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressiveSnippetsHeaderCard(
    snippets: List<Snippet>,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(MaterialShapes.ShapeClover4)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Snippets Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (snippets.size == 1) "1 variable ready" else "${snippets.size} variables ready for injection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Quick display of existing snippet variables chips
            if (snippets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    snippets.take(4).forEach { snip ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (snip.secure) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "$${snip.name}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (snippets.size > 4) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "+${snippets.size - 4} more",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
