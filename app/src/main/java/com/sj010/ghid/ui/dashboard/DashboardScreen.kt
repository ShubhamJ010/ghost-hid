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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.ComboOsPolicy
import com.sj010.ghid.domain.model.ConnectionStatus
import com.sj010.ghid.domain.model.HostOsFingerprint
import com.sj010.ghid.domain.model.PinnedFavoritesPolicy
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.os.HostOsClassifier
import com.sj010.ghid.domain.model.Snippet
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.theme.ExpressiveShapes
import com.sj010.ghid.ui.common.os.OsGlyph
import com.sj010.ghid.ui.common.shade.PullDownShade
import com.sj010.ghid.ui.common.shade.ShadeScope
import com.sj010.ghid.ui.quicksend.QuickSendOverlay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val targetOsFingerprint by service?.targetOs?.collectAsState()
        ?: remember { mutableStateOf(HostOsFingerprint.Unknown) }
    val cachedFingerprint = remember(targetDevice) {
        targetDevice?.address?.let { GhostHidApp.instance.deviceOsCache.get(it) }
    }
    val effectiveOs = remember(targetOsFingerprint, cachedFingerprint, targetDevice) {
        if (targetOsFingerprint.isKnown) {
            targetOsFingerprint.os
        } else if (cachedFingerprint?.isKnown == true) {
            cachedFingerprint.os
        } else if (targetDevice != null) {
            val name = try { targetDevice?.name } catch (_: SecurityException) { null }
            val byName = HostOsClassifier.fromName(name)
            if (byName?.isKnown == true) {
                byName.os
            } else if (HostOsClassifier.isAppleOui(targetDevice?.address)) {
                TargetOs.MACOS
            } else {
                TargetOs.WINDOWS
            }
        } else {
            TargetOs.WINDOWS
        }
    }
    val connectedOs = effectiveOs
    val targetSelectionReady by service?.targetSelectionReady?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val needsInitialTargetSelection by service?.needsInitialTargetSelection?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val comboRepo = GhostHidApp.instance.comboRepository
    val allCombos by comboRepo.combos.collectAsState()
    val comboUseCounts by GhostHidApp.instance.comboUsageRepository.useCounts.collectAsState()
    val pinnedFavorites by GhostHidApp.instance.pinnedFavoritesRepository.pins.collectAsState()
    val pinnedCombos = remember(pinnedFavorites, allCombos, connectedOs) {
        PinnedFavoritesPolicy.resolve(pinnedFavorites, allCombos)
            .filterValues { ComboOsPolicy.matches(it, connectedOs) }
    }
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

    LaunchedEffect(service, targetSelectionReady, needsInitialTargetSelection) {
        if (service != null && targetSelectionReady && needsInitialTargetSelection) {
            showDevicePicker = true
        }
    }


    PullDownShade(
        dock = { scope ->
            MorphingDeviceCard(
                progress = scope.progress,
                connectionState = connectionState,
                targetDevice = targetDevice,
                targetOs = connectedOs,
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
            // SECTION 2: FAV PINNED COLLAGE
            // ==========================================
            item {
                FavPinnedCollage(
                    combos = allCombos,
                    pinnedBySlot = pinnedCombos,
                    connectedOs = connectedOs,
                    hasSelectedDevice = targetDevice != null,
                    onRunCombo = { combo ->
                        service?.executeCombo(combo.effectiveSequence)
                    },
                    onPinCombo = { slotId, comboName ->
                        GhostHidApp.instance.pinnedFavoritesRepository.pin(slotId, comboName)
                    },
                    onBrowseCombos = { onNavigateToCombos?.invoke() }
                )
            }

            item {
                MostUsedCombosSection(
                    combos = allCombos,
                    useCounts = comboUseCounts,
                    connectedOs = connectedOs,
                    onRunCombo = { combo ->
                        service?.executeCombo(combo.effectiveSequence)
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
                shape = ExpressiveShapes.Clover4Leaf,
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
            initialTargetOs = connectedOs, targetDeviceOverride = targetDevice,
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
                            val cachedOs = remember(dev.address) {
                                GhostHidApp.instance.deviceOsCache.get(dev.address)?.os
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = {
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
                                        if (cachedOs != null && cachedOs.isKnown) {
                                            OsGlyph(
                                                os = cachedOs,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Computer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
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
