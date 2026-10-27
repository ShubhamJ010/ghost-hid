package com.sj010.ghid.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.AppSettings
import com.sj010.ghid.domain.model.OsOverrideMode
import com.sj010.ghid.domain.model.ThemeMode
import com.sj010.ghid.ui.theme.ExpressiveShapes
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = GhostHidApp.instance.settingsRepository
    val settings by repository.settings.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showResetDialog by remember { mutableStateOf(false) }

    // Keep the settings list compact: only one category can be open at a time.
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset All Settings?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will restore pointer sensitivity, typing delays, OS detection, and appearance options to their default factory values.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        repository.resetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Defaults"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category 1: Trackpad & Cursor
            item(key = "cat_trackpad") {
                val isExpanded = expandedCategory == "trackpad"
                ExpressiveCategoryCard(
                    title = "Trackpad & Cursor",
                    subtitle = "Pointer sensitivity, scrolling speed, and touch dynamics",
                    icon = Icons.Default.Mouse,
                    iconShape = ExpressiveShapes.Cookie9Sided,
                    categoryColor = MaterialTheme.colorScheme.primary,
                    categoryContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    categoryOnContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedCategory = if (isExpanded) null else "trackpad" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Pointer Sensitivity
                        ExpressiveSliderSetting(
                            title = "Pointer Sensitivity",
                            description = "Cursor speed multiplier for virtual trackpad touch drag",
                            value = settings.trackpadPointerSensitivity,
                            valueRange = 0.5f..3.5f,
                            steps = 30,
                            valueDisplay = String.format("%.1f×", settings.trackpadPointerSensitivity),
                            accentColor = MaterialTheme.colorScheme.primary,
                            onValueChange = { newVal ->
                                repository.update { it.copy(trackpadPointerSensitivity = (newVal * 10f).roundToInt() / 10f) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Scroll Rail Step Distance
                        ExpressiveSliderSetting(
                            title = "Scroll Step Sensitivity",
                            description = "Touch travel required to produce one scroll wheel notch",
                            value = settings.trackpadScrollStepDp,
                            valueRange = 10f..60f,
                            steps = 10,
                            valueDisplay = "${settings.trackpadScrollStepDp.roundToInt()} dp",
                            accentColor = MaterialTheme.colorScheme.primary,
                            onValueChange = { newVal ->
                                repository.update { it.copy(trackpadScrollStepDp = (newVal / 5f).roundToInt() * 5f) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Scroll Wheel Notches per Arrow Tap
                        ExpressiveSliderSetting(
                            title = "Scroll Button Step Notches",
                            description = "Wheel notches emitted per tap on up/down arrow buttons",
                            value = settings.trackpadScrollStepNotches.toFloat(),
                            valueRange = 1f..10f,
                            steps = 9,
                            valueDisplay = "${settings.trackpadScrollStepNotches} notches",
                            accentColor = MaterialTheme.colorScheme.primary,
                            onValueChange = { newVal ->
                                repository.update { it.copy(trackpadScrollStepNotches = newVal.roundToInt()) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Tap Duration Threshold
                        ExpressiveSliderSetting(
                            title = "Tap-to-Click Max Duration",
                            description = "Maximum touch duration (ms) registered as a quick click vs a drag",
                            value = settings.trackpadTapMaxDurationMs.toFloat(),
                            valueRange = 150f..500f,
                            steps = 14,
                            valueDisplay = "${settings.trackpadTapMaxDurationMs} ms",
                            accentColor = MaterialTheme.colorScheme.primary,
                            onValueChange = { newVal ->
                                repository.update { it.copy(trackpadTapMaxDurationMs = (newVal / 25f).roundToInt() * 25L) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Natural / Invert Scroll
                        ExpressiveSwitchSetting(
                            title = "Invert Scroll (Natural Scrolling)",
                            description = "Reverse vertical scroll wheel direction (macOS trackpad style)",
                            checked = settings.trackpadInvertScroll,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(trackpadInvertScroll = checked) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Tap to Click Toggle
                        ExpressiveSwitchSetting(
                            title = "Tap to Click",
                            description = "Single tap anywhere on virtual pad triggers left click",
                            checked = settings.trackpadTapToClick,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(trackpadTapToClick = checked) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Trackpad Haptics
                        ExpressiveSwitchSetting(
                            title = "Trackpad Haptic Feedback",
                            description = "Vibration response on clicks, gestures, and scroll notches",
                            checked = settings.trackpadHaptics,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(trackpadHaptics = checked) }
                            }
                        )
                    }
                }
            }

            // Category 2: Keyboard & HID Transmission Timing
            item(key = "cat_hid") {
                val isExpanded = expandedCategory == "hid"
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
                val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer

                ExpressiveCategoryCard(
                    title = "Typing & HID Transmission",
                    subtitle = "Keystroke delays, chord hold timing, and packet settle gaps",
                    icon = Icons.Default.Keyboard,
                    iconShape = ExpressiveShapes.Clover4Leaf,
                    categoryColor = tertiaryColor,
                    categoryContainerColor = tertiaryContainer,
                    categoryOnContainerColor = onTertiaryContainer,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedCategory = if (isExpanded) null else "hid" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Character Typing Delay
                        ExpressiveSliderSetting(
                            title = "Inter-Key Typing Delay",
                            description = "Delay between consecutive characters when typing snippets or sequences (increase if host drops characters)",
                            value = settings.typingDelayMs.toFloat(),
                            valueRange = 10f..80f,
                            steps = 14,
                            valueDisplay = "${settings.typingDelayMs} ms",
                            accentColor = tertiaryColor,
                            onValueChange = { newVal ->
                                repository.update { it.copy(typingDelayMs = (newVal / 5f).roundToInt() * 5L) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Key Chord Hold Duration
                        ExpressiveSliderSetting(
                            title = "Key Chord Hold Duration",
                            description = "Duration modifiers and key combinations remain held before release",
                            value = settings.keyHoldDurationMs.toFloat(),
                            valueRange = 20f..150f,
                            steps = 13,
                            valueDisplay = "${settings.keyHoldDurationMs} ms",
                            accentColor = tertiaryColor,
                            onValueChange = { newVal ->
                                repository.update { it.copy(keyHoldDurationMs = (newVal / 10f).roundToInt() * 10L) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Raw Report Hold Duration
                        ExpressiveSliderSetting(
                            title = "Raw HID Report Hold Time",
                            description = "Automatic release window for raw USB/Bluetooth HID reports",
                            value = settings.rawHoldDurationMs.toFloat(),
                            valueRange = 30f..200f,
                            steps = 17,
                            valueDisplay = "${settings.rawHoldDurationMs} ms",
                            accentColor = tertiaryColor,
                            onValueChange = { newVal ->
                                repository.update { it.copy(rawHoldDurationMs = (newVal / 10f).roundToInt() * 10L) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Action Sequence Step Gap
                        ExpressiveSliderSetting(
                            title = "Macro Inter-Step Settle Gap",
                            description = "Buffer time between steps in complex macros and payload execution",
                            value = settings.actionStepGapMs.toFloat(),
                            valueRange = 10f..100f,
                            steps = 9,
                            valueDisplay = "${settings.actionStepGapMs} ms",
                            accentColor = tertiaryColor,
                            onValueChange = { newVal ->
                                repository.update { it.copy(actionStepGapMs = (newVal / 10f).roundToInt() * 10L) }
                            }
                        )
                    }
                }
            }

            // Category 3: Host Connection & OS Policy
            item(key = "cat_host") {
                val isExpanded = expandedCategory == "host"
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
                val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer

                ExpressiveCategoryCard(
                    title = "Host Connection & OS Policy",
                    subtitle = "OS shortcut mapping mode, Bluetooth auto-connect, and LED fingerprinting",
                    icon = Icons.Default.Computer,
                    iconShape = ExpressiveShapes.Boom,
                    categoryColor = secondaryColor,
                    categoryContainerColor = secondaryContainer,
                    categoryOnContainerColor = onSecondaryContainer,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedCategory = if (isExpanded) null else "host" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // OS Override Mode
                        ExpressiveChoiceGroup(
                            title = "Target Host OS Override",
                            description = "Select whether OS shortcuts (Win/Cmd/Super, Lock, Terminal) adapt automatically or stay locked to a specific platform",
                            options = OsOverrideMode.entries.map { it.label },
                            selectedOption = settings.osOverride.label,
                            accentColor = secondaryColor,
                            onOptionSelected = { label ->
                                val selectedMode = OsOverrideMode.entries.firstOrNull { it.label == label } ?: OsOverrideMode.AUTO
                                repository.update { it.copy(osOverride = selectedMode) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Auto-Connect on App Launch
                        ExpressiveSwitchSetting(
                            title = "Auto-Connect on Launch",
                            description = "Automatically attempt Bluetooth HID connection to the last linked host computer when GhostHID starts",
                            checked = settings.autoConnectOnLaunch,
                            accentColor = secondaryColor,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(autoConnectOnLaunch = checked) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // LED Handshake Detection Window
                        ExpressiveSliderSetting(
                            title = "Passive OS Fingerprint Window",
                            description = "Initial timeout to capture Windows Caps/Num Lock handshake reports",
                            value = settings.ledHandshakeWindowMs.toFloat(),
                            valueRange = 200f..1500f,
                            steps = 13,
                            valueDisplay = "${settings.ledHandshakeWindowMs} ms",
                            accentColor = secondaryColor,
                            onValueChange = { newVal ->
                                repository.update { it.copy(ledHandshakeWindowMs = (newVal / 100f).roundToInt() * 100L) }
                            }
                        )
                    }
                }
            }

            // Category 4: Display & System Appearance
            item(key = "cat_appearance") {
                val isExpanded = expandedCategory == "appearance"
                // Expressive mint/emerald styling
                val mintAccent = Color(0xFF00BFA5)
                val mintContainer = MaterialTheme.colorScheme.surfaceContainerHigh
                val onMintContainer = MaterialTheme.colorScheme.onSurface

                ExpressiveCategoryCard(
                    title = "Display & Appearance",
                    subtitle = "Color theming, Dynamic Color, 120Hz refresh, and screen wakelock",
                    icon = Icons.Default.Palette,
                    iconShape = ExpressiveShapes.Sunny,
                    categoryColor = mintAccent,
                    categoryContainerColor = mintContainer,
                    categoryOnContainerColor = onMintContainer,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedCategory = if (isExpanded) null else "appearance" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Theme Mode
                        ExpressiveChoiceGroup(
                            title = "Color Theme",
                            description = "Select System default, always Dark, or Light appearance",
                            options = ThemeMode.entries.map { it.label },
                            selectedOption = settings.themeMode.label,
                            accentColor = mintAccent,
                            onOptionSelected = { label ->
                                val mode = ThemeMode.entries.firstOrNull { it.label == label } ?: ThemeMode.SYSTEM
                                repository.update { it.copy(themeMode = mode) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Dynamic Color (Material You)
                        ExpressiveSwitchSetting(
                            title = "Material You Dynamic Color",
                            description = "Extract expressive color palette from your system wallpaper (Android 12+)",
                            checked = settings.dynamicColor,
                            accentColor = mintAccent,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(dynamicColor = checked) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Keep Screen Awake
                        ExpressiveSwitchSetting(
                            title = "Keep Screen Awake",
                            description = "Prevent device screen from turning off while using virtual trackpad or stream deck",
                            checked = settings.keepScreenAwake,
                            accentColor = mintAccent,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(keepScreenAwake = checked) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // 120Hz High Refresh Rate
                        ExpressiveSwitchSetting(
                            title = "Prefer 120Hz Ultra-Smooth Refresh",
                            description = "Enables hardware high-refresh-rate mode for ultra-low latency cursor response",
                            checked = settings.highRefreshRate,
                            accentColor = mintAccent,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(highRefreshRate = checked) }
                            }
                        )
                    }
                }
            }

            // Category 5: Quick Send & Deck
            item(key = "cat_quicksend") {
                val isExpanded = expandedCategory == "quicksend"
                // Expressive coral/rose styling
                val roseAccent = Color(0xFFFF5252)
                val roseContainer = MaterialTheme.colorScheme.surfaceContainerHigh
                val onRoseContainer = MaterialTheme.colorScheme.onSurface

                ExpressiveCategoryCard(
                    title = "Quick Send & Stream Deck",
                    subtitle = "Composer behavior, recent history retention, and action triggers",
                    icon = Icons.Default.Bolt,
                    iconShape = ExpressiveShapes.Flower,
                    categoryColor = roseAccent,
                    categoryContainerColor = roseContainer,
                    categoryOnContainerColor = onRoseContainer,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedCategory = if (isExpanded) null else "quicksend" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Quick Send Auto-Clear
                        ExpressiveSwitchSetting(
                            title = "Auto-Clear Input After Send",
                            description = "Automatically clear quick-send text input box after sending to host",
                            checked = settings.quickSendAutoClear,
                            accentColor = roseAccent,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(quickSendAutoClear = checked) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // History Limit
                        ExpressiveChoiceGroup(
                            title = "Recent History Retention Limit",
                            description = "Maximum dispatched quick-send sequences retained for instant rerun",
                            options = listOf("10", "25", "50", "100"),
                            selectedOption = settings.quickSendHistoryLimit.toString(),
                            accentColor = roseAccent,
                            onOptionSelected = { label ->
                                val limit = label.toIntOrNull() ?: 25
                                repository.update { it.copy(quickSendHistoryLimit = limit) }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Quick Send Haptics
                        ExpressiveSwitchSetting(
                            title = "Send Button Haptic Feedback",
                            description = "Vibrate when dispatching quick-send commands or triggering deck items",
                            checked = settings.quickSendHaptics,
                            accentColor = roseAccent,
                            onCheckedChange = { checked ->
                                repository.update { it.copy(quickSendHaptics = checked) }
                            }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Material 3 Expressive Category Card Container
// -----------------------------------------------------------------------------

@Composable
private fun ExpressiveCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconShape: Shape,
    categoryColor: Color,
    categoryContainerColor: Color,
    categoryOnContainerColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 16.dp,
            bottomEnd = 24.dp,
            bottomStart = 16.dp
        ),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(iconShape)
                            .background(categoryColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(150)) + expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ),
                exit = fadeOut(animationSpec = tween(100)) + shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 6.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Setting Row Components (Slider, Switch, Choice Chips)
// -----------------------------------------------------------------------------

@Composable
private fun ExpressiveSliderSetting(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expressive Value Pill Badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExpressiveSwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpressiveChoiceGroup(
    title: String,
    description: String,
    options: List<String>,
    selectedOption: String,
    accentColor: Color,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onOptionSelected(option) },
                    label = {
                        Text(
                            text = option,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(),
                    shape = CircleShape
                )
            }
        }
    }
}
