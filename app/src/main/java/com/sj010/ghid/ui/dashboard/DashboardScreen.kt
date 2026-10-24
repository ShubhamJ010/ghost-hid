package com.sj010.ghid.ui.dashboard

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.ConnectionStatus
import com.sj010.ghid.domain.model.Snippet
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.theme.MaterialShapes
import com.sj010.ghid.ui.common.shade.PullDownShade
import com.sj010.ghid.ui.common.shade.ShadeScope
import com.sj010.ghid.ui.quicksend.QuickSendOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    service: HidService?,
    onNavigateToCombos: (() -> Unit)? = null,
    onQuit: () -> Unit = {}
) {
    val connectionState by service?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    val targetDevice by service?.targetDeviceState?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val targetSelectionReady by service?.targetSelectionReady?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val needsInitialTargetSelection by service?.needsInitialTargetSelection?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val comboRepo = GhostHidApp.instance.comboRepository
    val allCombos by comboRepo.combos.collectAsState()
    val comboUseCounts by GhostHidApp.instance.comboUsageRepository.useCounts.collectAsState()
    val dashboardListState = rememberLazyListState()
    val showQuickSendFab by remember {
        derivedStateOf {
            dashboardListState.firstVisibleItemIndex == 0 &&
                dashboardListState.firstVisibleItemScrollOffset == 0
        }
    }
    val showQuitButton by remember {
        derivedStateOf {
            DashboardScrollPolicy.showQuitButton(
                itemCount = dashboardListState.layoutInfo.totalItemsCount,
                canScrollForward = dashboardListState.canScrollForward
            )
        }
    }

    var showDevicePicker by remember { mutableStateOf(false) }
    var showQuickSend by remember { mutableStateOf(false) }
    var lastExecutedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(service, targetSelectionReady, needsInitialTargetSelection) {
        if (service != null && targetSelectionReady && needsInitialTargetSelection) {
            showDevicePicker = true
        }
    }

    LaunchedEffect(lastExecutedStatus) {
        if (lastExecutedStatus != null) {
            delay(2800)
            lastExecutedStatus = null
        }
    }

    PullDownShade(
        dock = { scope ->
            MorphingDeviceCard(
                progress = scope.progress,
                connectionState = connectionState,
                targetDevice = targetDevice,
                onConnect = { service?.connectTarget() },
                onDisconnect = { service?.disconnectTarget() },
                onSwitchDevice = { showDevicePicker = true },
                onOpen = { scope.open() },
                onDismiss = { scope.dismiss() }
            )
        },
        shade = {},
        dockHeight = 104.dp,
        openDockHeight = 220.dp,
        openHeight = 240.dp,
        pushContentBelowShade = true,
        drawBorder = false,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = dashboardListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // SECTION 1: APP HEADING
            // ==========================================
            item {
                val hidAccent = MaterialTheme.colorScheme.primary
                val heading = remember(hidAccent) {
                    buildAnnotatedString {
                        append("Ghost")
                        withStyle(androidx.compose.ui.text.SpanStyle(color = hidAccent)) {
                            append("HID")
                        }
                    }
                }
                Text(
                    text = heading,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            // ==========================================
            // SECTION 2: FAVORITE COMBOS (EXPRESSIVE SHAPES COLLAGE)
            // ==========================================
            item {
                ExpressiveFavoriteCombosSection(
                    combos = allCombos,
                    onRunCombo = { combo ->
                        service?.executeCombo(combo.keys)
                        lastExecutedStatus = "⚡ Executed: ${combo.name}"
                    },
                    onEmptySlotClick = { cfg ->
                        lastExecutedStatus = "⭐ Star a combo in Combos to pin to ${cfg.emptyLabel}"
                        onNavigateToCombos?.invoke()
                    }
                )
            }

            item {
                MostUsedCombosSection(
                    combos = allCombos,
                    useCounts = comboUseCounts,
                    onRunCombo = { combo ->
                        service?.executeCombo(combo.keys)
                        lastExecutedStatus = "⚡ Executed: ${combo.name}"
                    },
                    onViewAll = { onNavigateToCombos?.invoke() }
                )
            }

        }

        AnimatedVisibility(
            visible = showQuitButton,
            enter = fadeIn(animationSpec = tween(450)) +
                slideInVertically(
                    animationSpec = tween(450, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                ),
            exit = fadeOut(animationSpec = tween(200)) +
                slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { it / 2 }
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 18.dp,
                    bottomEnd = 28.dp,
                    bottomStart = 18.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 1.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "QUIT APP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // Floating Toast Banner
        AnimatedVisibility(
            visible = lastExecutedStatus != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = lastExecutedStatus ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showQuickSendFab,
            enter = scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ),
            exit = scaleOut(
                targetScale = 0f,
                animationSpec = tween(140, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            LargeFloatingActionButton(
                onClick = { showQuickSend = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialShapes.ShapeClover4,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Open quick send",
                    modifier = Modifier.size(38.dp)
                )
            }
        }
      }
    }

    if (showQuickSend) {
        QuickSendOverlay(
            service = service,
            onDismiss = { showQuickSend = false }
        )
    }

    // Bluetooth Device Picker Dialog
    if (showDevicePicker) {
        val context = LocalContext.current
        val pairedDevices = service?.getPairedDevices() ?: emptyList()
        AlertDialog(
            onDismissRequest = {
                service?.markTargetSelectionComplete()
                showDevicePicker = false
            },
            icon = {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Select Target PC",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (pairedDevices.isEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "No paired Bluetooth devices detected. Put your computer in Bluetooth discovery, then pair your phone.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Bluetooth Settings", fontSize = 11.sp, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Discoverable", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(pairedDevices) { dev ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = {
                                    service?.selectTarget(dev.address)
                                    service?.connectTarget(dev.address)
                                    showDevicePicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Computer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dev.name ?: "Unknown Device",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dev.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        service?.markTargetSelectionComplete()
                        showDevicePicker = false
                    },
                    shape = CircleShape
                ) {
                    Text("Close")
                }
            }
        )
    }
}


// =========================================================================
// SECTION 2: FAVORITE COMBOS (M3 EXPRESSIVE FLAT SHAPES)
// =========================================================================

private data class ExpressiveFlatPalette(
    val container: Color,
    val content: Color,
    val border: Color
)

data class CollageConfig(
    val slotId: Int,
    val shapeName: String,
    val emptyLabel: String,
    val width: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp,
    val align: Alignment,
    val rot: Float,
    val shape: Shape,
    val offsetX: androidx.compose.ui.unit.Dp,
    val offsetY: androidx.compose.ui.unit.Dp,
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressiveFavoriteCombosSection(
    combos: List<Combo>,
    onRunCombo: (Combo) -> Unit,
    onEmptySlotClick: (CollageConfig) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    // Exactly the user's top favorite combos (up to 5 for the collage)
    val distinctFavorites = remember(combos) {
        val favs = combos.filter { it.favorite }.distinctBy { it.name }
        if (favs.isNotEmpty()) favs.take(5) else combos.distinctBy { it.name }.take(3)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // M3 Expressive Flat Collage Layout with 5 distinct shapes & well-spaced gaps
        val expressivePalettes = listOf(
            // 0: Deep Royal Violet / Lilac (Center Pill - Login)
            ExpressiveFlatPalette(
                container = Color(0xFF4B3280),
                content = Color(0xFFF3EDF7),
                border = Color(0xFFD0BCFF)
            ),
            // 1: Expressive Jade / Mint (TopStart Squircle - Lock)
            ExpressiveFlatPalette(
                container = Color(0xFF00513B),
                content = Color(0xFFA7F8D6),
                border = Color(0xFF86F8CE)
            ),
            // 2: Expressive Sunset Amber / Peach (BottomEnd 6-Point Star - Sleep)
            ExpressiveFlatPalette(
                container = Color(0xFF943C00),
                content = Color(0xFFFFDCC4),
                border = Color(0xFFFFB68F)
            ),
            // 3: Expressive Ocean Blue (TopEnd Fan - Fan)
            ExpressiveFlatPalette(
                container = Color(0xFF004C6E),
                content = Color(0xFFCCE6FF),
                border = Color(0xFF92CDFF)
            ),
            // 4: Expressive Raspberry / Rose (BottomStart Ghost-ish - Ghost-ish)
            ExpressiveFlatPalette(
                container = Color(0xFF8C1D40),
                content = Color(0xFFFFD9E2),
                border = Color(0xFFFFB1C8)
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            val p0 = expressivePalettes[0]
            val p1 = expressivePalettes[1]
            val p2 = expressivePalettes[2]
            val p3 = expressivePalettes[3]
            val p4 = expressivePalettes[4]

            // 5 Expressive shapes: scattered, organic layout with enlarged Lock and downward shifted elements
            // 0: Center Tilted Pill (Login - shifted down)
            // 1: TopStart Squircle (Lock - enlarged & shifted down)
            // 2: BottomEnd 6-Sided Star (Sleep - enlarged 10% & snug)
            // 3: TopEnd Fan (Fan)
            // 4: BottomStart Ghost-ish (Ghost-ish - shifted down)
            val configs = remember {
                listOf(
                    // 0: Center Tilted Pill (Login)
                    CollageConfig(
                        slotId = 0,
                        shapeName = "Pill",
                        emptyLabel = "Pill",
                        width = 110.dp,
                        height = 172.dp,
                        align = Alignment.Center,
                        rot = 42f,
                        shape = RoundedCornerShape(percent = 50),
                        offsetX = (-6).dp,
                        offsetY = 14.dp,
                        containerColor = p0.container,
                        contentColor = p0.content,
                        border = BorderStroke(1.5.dp, p0.border.copy(alpha = 0.40f))
                    ),
                    // 1: TopStart Squircle (Lock - enlarged to 126dp and shifted down)
                    CollageConfig(
                        slotId = 1,
                        shapeName = "Squircle",
                        emptyLabel = "Squircle",
                        width = 126.dp,
                        height = 126.dp,
                        align = Alignment.TopStart,
                        rot = -12f,
                        shape = RoundedCornerShape(30.dp),
                        offsetX = 12.dp,
                        offsetY = 38.dp,
                        containerColor = p1.container,
                        contentColor = p1.content,
                        border = BorderStroke(1.5.dp, p1.border.copy(alpha = 0.40f))
                    ),
                    // 2: BottomEnd 6-Sided Rounded Star (Sleep - enlarged 10% to 150dp, snug towards center)
                    CollageConfig(
                        slotId = 2,
                        shapeName = "Flower",
                        emptyLabel = "Flower",
                        width = 150.dp,
                        height = 150.dp,
                        align = Alignment.BottomEnd,
                        rot = 8f,
                        shape = com.sj010.ghid.ui.theme.RoundedStarShape(sides = 6, curve = 0.09, rotation = 45f),
                        offsetX = (-16).dp,
                        offsetY = (-10).dp,
                        containerColor = p2.container,
                        contentColor = p2.content,
                        border = BorderStroke(1.5.dp, p2.border.copy(alpha = 0.40f))
                    ),
                    // 3: TopEnd Fan (M3 Expressive ShapeFan)
                    CollageConfig(
                        slotId = 3,
                        shapeName = "Fan",
                        emptyLabel = "Fan",
                        width = 112.dp,
                        height = 112.dp,
                        align = Alignment.TopEnd,
                        rot = -4f,
                        shape = MaterialShapes.ShapeFan,
                        offsetX = (-14).dp,
                        offsetY = 10.dp,
                        containerColor = p3.container,
                        contentColor = p3.content,
                        border = BorderStroke(1.5.dp, p3.border.copy(alpha = 0.40f))
                    ),
                    // 4: BottomStart Ghost-ish (M3 Expressive ShapeGhostish - shifted down & scattered)
                    CollageConfig(
                        slotId = 4,
                        shapeName = "Ghost-ish",
                        emptyLabel = "Ghost-ish",
                        width = 114.dp,
                        height = 114.dp,
                        align = Alignment.BottomStart,
                        rot = 6f,
                        shape = MaterialShapes.ShapeGhostish,
                        offsetX = 18.dp,
                        offsetY = 4.dp,
                        containerColor = p4.container,
                        contentColor = p4.content,
                        border = BorderStroke(1.5.dp, p4.border.copy(alpha = 0.40f))
                    )
                )
            }

            configs.forEachIndexed { idx, cfg ->
                val combo = distinctFavorites.getOrNull(idx)
                if (combo != null) {
                    CollageShapeButton(
                        combo = combo,
                        config = cfg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRunCombo(combo)
                        }
                    )
                } else {
                    CollageEmptyShapeSlot(
                        config = cfg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEmptySlotClick(cfg)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CollageShapeButton(
    combo: Combo,
    config: CollageConfig,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "shapeScale"
    )

    Box(
        modifier = Modifier
            .size(config.width, config.height)
            .align(config.align)
            .offset(config.offsetX, config.offsetY)
            .scale(animatedScale)
            .rotate(config.rot)
            .clip(config.shape)
            .background(config.containerColor)
            .border(config.border, config.shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .rotate(-config.rot)
                .padding(6.dp)
        ) {
            val emojiSize = when {
                config.width >= 125.dp || config.height >= 160.dp -> 32.sp
                config.width >= 105.dp -> 28.sp
                else -> 24.sp
            }
            val titleStyle = when {
                config.width >= 125.dp || config.height >= 160.dp -> MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                config.width >= 105.dp -> MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp)
                else -> MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
            }

            Text(
                text = combo.emoji?.ifEmpty { "⚡" } ?: "⚡",
                fontSize = emojiSize
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = combo.name,
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = config.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun BoxScope.CollageEmptyShapeSlot(
    config: CollageConfig,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "emptyShapeScale"
    )

    Box(
        modifier = Modifier
            .size(config.width, config.height)
            .align(config.align)
            .offset(config.offsetX, config.offsetY)
            .scale(animatedScale)
            .rotate(config.rot)
            .clip(config.shape)
            .background(config.containerColor.copy(alpha = 0.22f))
            .border(
                BorderStroke(1.5.dp, config.border.brush.let { config.contentColor.copy(alpha = 0.55f) }),
                config.shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .rotate(-config.rot)
                .padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add favorite combo to ${config.emptyLabel}",
                modifier = Modifier.size(26.dp),
                tint = config.contentColor.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = config.emptyLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = config.contentColor.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
