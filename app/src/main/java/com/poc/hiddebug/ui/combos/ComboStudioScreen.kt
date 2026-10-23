package com.poc.hiddebug.ui.combos

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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import com.poc.hiddebug.GhostHidApp
import com.poc.hiddebug.domain.model.Combo
import com.poc.hiddebug.domain.model.HidStep
import com.poc.hiddebug.domain.model.Snippet
import com.poc.hiddebug.domain.parser.ActionSequenceParser
import com.poc.hiddebug.service.HidService
import com.poc.hiddebug.ui.theme.Clover4Shape
import com.poc.hiddebug.ui.theme.MaterialShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ComboFilterCategory(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    SNIPPETS("Vault"),
    TIMED("Timed")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComboStudioScreen(
    service: HidService?,
    onNavigateToSnippets: (() -> Unit)? = null
) {
    val repo = GhostHidApp.instance.comboRepository
    val combos by repo.combos.collectAsState()

    val snippetRepo = GhostHidApp.instance.snippetRepository
    val snippets by snippetRepo.snippets.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ComboFilterCategory.ALL) }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingCombo by remember { mutableStateOf<Combo?>(null) }
    var comboToDelete by remember { mutableStateOf<Combo?>(null) }

    var executingComboName by remember { mutableStateOf<String?>(null) }
    var bannerStatus by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Floating notification timeout
    LaunchedEffect(bannerStatus) {
        if (bannerStatus != null) {
            delay(2800)
            bannerStatus = null
        }
    }

    // Filter and search logic
    val filteredCombos = remember(combos, searchQuery, selectedFilter) {
        combos.filter { combo ->
            // Search query matches name, description, emoji, or keys
            val matchesQuery = searchQuery.isBlank() ||
                combo.name.contains(searchQuery, ignoreCase = true) ||
                (combo.description?.contains(searchQuery, ignoreCase = true) == true) ||
                (combo.emoji?.contains(searchQuery) == true) ||
                combo.keys.contains(searchQuery, ignoreCase = true)

            if (!matchesQuery) return@filter false

            // Category filter
            when (selectedFilter) {
                ComboFilterCategory.ALL -> true
                ComboFilterCategory.FAVORITES -> combo.favorite
                ComboFilterCategory.SNIPPETS -> combo.keys.contains("$")
                ComboFilterCategory.TIMED -> combo.keys.contains("->")
            }
        }.sortedWith(
            compareByDescending<Combo> { it.favorite }
                .thenBy { it.name.lowercase() }
        )
    }

    // Category counts for quick tabs
    val favoriteCount = remember(combos) { combos.count { it.favorite } }
    val snippetComboCount = remember(combos) { combos.count { it.keys.contains("$") } }
    val timedComboCount = remember(combos) { combos.count { it.keys.contains("->") } }

    Scaffold(
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
                // SECTION 1: TOP EXPRESSIVE SNIPPET VAULT CARD
                // ==========================================
                item {
                    ExpressiveSnippetsHeaderCard(
                        snippets = snippets,
                        onClick = { onNavigateToSnippets?.invoke() }
                    )
                }

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
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Search combos, keys, or tags...",
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
                                onClick = { selectedFilter = ComboFilterCategory.ALL }
                            )

                            ComboFilterChip(
                                label = "★ Favs ($favoriteCount)",
                                selected = selectedFilter == ComboFilterCategory.FAVORITES,
                                onClick = { selectedFilter = ComboFilterCategory.FAVORITES }
                            )

                            ComboFilterChip(
                                label = "$ Vault ($snippetComboCount)",
                                selected = selectedFilter == ComboFilterCategory.SNIPPETS,
                                onClick = { selectedFilter = ComboFilterCategory.SNIPPETS }
                            )

                            ComboFilterChip(
                                label = "⏱ Timed ($timedComboCount)",
                                selected = selectedFilter == ComboFilterCategory.TIMED,
                                onClick = { selectedFilter = ComboFilterCategory.TIMED }
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 3: COMBO CARDS LIST OR EMPTY STATE
                // ==========================================
                if (filteredCombos.isEmpty()) {
                    item {
                        EmptyCombosState(
                            hasSearchQuery = searchQuery.isNotBlank() || selectedFilter != ComboFilterCategory.ALL,
                            onClearFilters = {
                                searchQuery = ""
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
                                bannerStatus = "⚡ Executing: ${combo.name}"

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
                                bannerStatus = if (!combo.favorite) "★ Starred ${combo.name}" else "Unstarred ${combo.name}"
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

            // ==========================================
            // FLOATING STATUS BANNER (Dynamic Material Pill)
            // ==========================================
            AnimatedVisibility(
                visible = bannerStatus != null,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(180)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerStatus ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
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
                        bannerStatus = "Deleted ${target.name}"
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
        ComboEditorBottomSheet(
            initial = editingCombo,
            availableSnippets = snippets,
            onDismiss = {
                showEditorSheet = false
            },
            onSave = { saved ->
                repo.addOrUpdate(saved)
                bannerStatus = "Saved ${saved.name}"
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
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
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

    // Bouncy heart scale animation
    val favScale by animateFloatAsState(
        targetValue = if (combo.favorite) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "favScale"
    )

    // Parse steps and compute total estimated delay
    val steps = remember(combo.keys) { ActionSequenceParser.parse(combo.keys) }
    val totalDelayMs = remember(steps) {
        steps.filterIsInstance<HidStep.Delay>().sumOf { it.millis }
    }

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
            // Header: Expressive Emoji Shape + Title + Subtitle + Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Expressive shape badge for emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(expressiveShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = combo.emoji ?: "⚡",
                            fontSize = 22.sp
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

                            if (totalDelayMs > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = "~${"%.1f".format(totalDelayMs / 1000.0)}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
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

            Spacer(modifier = Modifier.height(12.dp))

            // Step Badges Flow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                steps.forEach { step ->
                    StepBadge(step = step)
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
}

// =========================================================================
// STEP BADGE (RICH AST TOKENS WITH SEMANTIC ICONS & TONAL ROLES)
// =========================================================================

@Composable
fun StepBadge(step: HidStep) {
    val (icon, label, bg, fg) = when (step) {
        is HidStep.KeyboardChord -> Quadruple(
            Icons.Default.Keyboard,
            step.label,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is HidStep.TypeText -> Quadruple(
            Icons.Default.TextFields,
            "\"${step.text}\"",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        is HidStep.Variable -> Quadruple(
            Icons.Default.Key,
            "$${step.name}",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        is HidStep.Delay -> Quadruple(
            Icons.Default.Timer,
            "${"%.1f".format(step.millis / 1000.0)}s",
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        is HidStep.MouseAction -> Quadruple(
            Icons.Default.Mouse,
            step.action,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        is HidStep.Directive -> Quadruple(
            Icons.Default.Warning,
            "!${step.name}",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// =========================================================================
// EMPTY STATE COMPONENT
// =========================================================================

@Composable
private fun EmptyCombosState(
    hasSearchQuery: Boolean,
    onClearFilters: () -> Unit,
    onCreateNew: () -> Unit
) {
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
            text = if (hasSearchQuery) "No matching combos" else "No combos found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (hasSearchQuery) "Try clearing search filters to see all combos." else "Tap below to create your first automated keystroke sequence.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (hasSearchQuery) {
            Button(
                onClick = onClearFilters,
                shape = CircleShape
            ) {
                Text("Clear Filters")
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

// =========================================================================
// COMBO EDITOR BOTTOM SHEET (LIVE AST PREVIEW & TOKEN CHIPS INJECTOR)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComboEditorBottomSheet(
    initial: Combo?,
    availableSnippets: List<Snippet>,
    onDismiss: () -> Unit,
    onSave: (Combo) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "⚡") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var keys by remember { mutableStateOf(initial?.keys ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val quickEmojis = listOf("⚡", "🔑", "🔒", "🌙", "💻", "🚀", "🛡️", "🎮", "⚙️", "🖱️")

    val validationResult = remember(keys) { ActionSequenceParser.validate(keys) }
    val parsedSteps = remember(keys) {
        if (keys.isBlank()) emptyList()
        else ActionSequenceParser.parse(keys)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initial == null) "New Combo" else "Edit Combo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Validation badge
                if (keys.isNotBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = if (validationResult.isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (validationResult.isSuccess) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (validationResult.isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (validationResult.isSuccess) "Syntax OK" else "Syntax Error",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (validationResult.isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Quick emoji selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickEmojis.forEach { em ->
                    val isSelected = emoji == em
                    Surface(
                        onClick = { emoji = em },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = em, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Name & Emoji text inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Icon") },
                    modifier = Modifier.width(76.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Combo Name") },
                    placeholder = { Text("e.g. Open Terminal") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Short note on what this combo does") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Keys Sequence Grammar Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            Text(
                                "Delays: ->{sec} | Strings: type:<text> | Vault: \$VAR | Chords: win+l",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick Token Injector Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTokens = listOf(
                        "win+r",
                        "->{0.5}",
                        "enter",
                        "tab",
                        "esc",
                        "ctrl+alt+del",
                        "win+l",
                        "type:"
                    )

                    quickTokens.forEach { token ->
                        Surface(
                            onClick = {
                                keys = if (keys.isBlank()) token
                                else if (token.startsWith("->")) "$keys $token"
                                else if (keys.endsWith("->{0.5}") || keys.endsWith("->")) "$keys $token"
                                else "$keys ->{0.3} $token"
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "+ $token",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Available snippets injection
                    availableSnippets.forEach { snip ->
                        Surface(
                            onClick = {
                                val varToken = "$${snip.name}"
                                keys = if (keys.isBlank()) varToken else "$keys $varToken"
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "+ $${snip.name}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Live AST Step Badges Preview
            if (parsedSteps.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "LIVE AST PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            parsedSteps.forEach { step ->
                                StepBadge(step = step)
                            }
                        }
                    }
                }
            }

            // Action Buttons: Cancel and Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    shape = CircleShape
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(10.dp))

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
                    Text("Save Combo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
