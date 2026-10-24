package com.sj010.ghid.ui.quicksend

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.R
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.parser.SequenceAnalysis
import com.sj010.ghid.domain.parser.SequenceAnalyzer
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.combos.editor.ComboEditorScreen
import com.sj010.ghid.ui.common.os.OsGlyph
import com.sj010.ghid.ui.common.sequence.SequencePreview
import com.sj010.ghid.ui.common.sequence.formatStepCount
import com.sj010.ghid.ui.theme.MaterialShapes
import kotlinx.coroutines.launch

/**
 * Dedicated quick-send surface. The composer lives at the bottom and rises with the
 * keyboard; the live parsed preview is always directly above the field.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickSendOverlay(
    service: HidService?,
    initialTargetOs: TargetOs? = null,
    targetDeviceOverride: BluetoothDevice? = null,
    onDismiss: () -> Unit
) {
    val historyRepository = GhostHidApp.instance.quickSendHistoryRepository
    val comboRepository = GhostHidApp.instance.comboRepository
    val history by historyRepository.entries.collectAsState()

    val serviceTargetDevice by service?.targetDeviceState?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val targetDevice = targetDeviceOverride ?: serviceTargetDevice

    val detectedOs by service?.effectiveTargetOs?.collectAsState()
        ?: remember { mutableStateOf(initialTargetOs ?: TargetOs.WINDOWS) }
    val currentOs = detectedOs

    var scheme by rememberSaveable { mutableStateOf("") }
    var editorScheme by remember { mutableStateOf<String?>(null) }

    val analysis = remember(scheme, currentOs) { SequenceAnalyzer.analyze(scheme, currentOs) }
    val imeVisible = WindowInsets.isImeVisible
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val historyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var composerVisible by rememberSaveable { mutableStateOf(true) }
    val composerScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                return when {
                    available.y < -4f -> {
                        composerVisible = false
                        Offset.Zero
                    }
                    available.y > 4f && !composerVisible -> {
                        composerVisible = true
                        available
                    }
                    available.y > 4f -> {
                        composerVisible = true
                        Offset.Zero
                    }
                    else -> Offset.Zero
                }
            }
        }
    }

    fun revealComposerAtTop() {
        composerVisible = true
        scope.launch { historyListState.animateScrollToItem(0) }
    }

    fun send(raw: String, record: Boolean) {
        val target = SequenceAnalyzer.analyze(raw, currentOs)
        if (!target.canSend) {
            return
        }
        if (service == null) {
            return
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        service.executeCombo(raw)
        if (record) historyRepository.record(raw)
        revealComposerAtTop()
        focusManager.clearFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            QuickSendSheetHeader(
                targetOs = currentOs,
                targetDevice = targetDevice,
                onDismiss = onDismiss,
                historyCount = history.size
            )

            if (!imeVisible) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(composerScrollConnection),
                    state = historyListState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickSendHistoryHeader(count = history.size)
                    }

                    if (history.isEmpty()) {
                        item {
                            QuickSendEmptyState(
                                onPickExample = { picked -> scheme = picked },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(history, key = { it.createdAt + it.scheme }) { entry ->
                            QuickSendHistoryCard(
                                entry = entry,
                                onRerun = {
                                    scheme = entry.scheme
                                    send(entry.scheme, record = true)
                                },
                                onSave = {
                                    focusManager.clearFocus()
                                    editorScheme = entry.scheme
                                },
                                targetOs = currentOs
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            AnimatedContent(
                targetState = imeVisible || composerVisible,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(animationSpec = spring()) + scaleIn(initialScale = 0.92f))
                        .togetherWith(fadeOut(animationSpec = spring()) + scaleOut(targetScale = 0.92f))
                        .using(
                            SizeTransform(
                                clip = false,
                                sizeAnimationSpec = { _, _ ->
                                    spring(dampingRatio = 0.82f, stiffness = 380f)
                                }
                            )
                        )
                },
                label = "quickSendComposerMorph"
            ) {
                if (it) {
                    QuickSendComposerDock(
                        scheme = scheme,
                        analysis = analysis,
                        compactPreview = imeVisible,
                        targetOs = currentOs,
                        onSchemeChange = { scheme = it },
                        onClear = { scheme = "" },
                        onPaste = {
                            val clip = clipboard.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                scheme = clip.trim()
                            }
                        },
                        onSend = { send(scheme, record = true) }
                    )
                } else {
                    QuickSendCollapsedButton(
                        targetOs = currentOs,
                        targetDevice = targetDevice,
                        onClick = ::revealComposerAtTop
                    )
                }
            }
        }
    }

    editorScheme?.let { prefill ->
        ComboEditorScreen(
            initial = null,
            initialScheme = prefill,
            defaultOs = currentOs,
            service = service,
            onDismiss = { editorScheme = null },
            onSave = { combo ->
                comboRepository.addOrUpdate(combo)
                editorScheme = null
            }
        )
    }
}

@Composable
private fun QuickSendCollapsedButton(
    targetOs: TargetOs,
    targetDevice: BluetoothDevice?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "quickSendButtonPress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .width(228.dp)
                .height(72.dp)
                .scale(buttonScale),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(MaterialShapes.ShapeClover4)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (targetDevice != null) {
                        OsGlyph(
                            os = targetOs,
                            contentDescription = targetOs.label,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification_ghost),
                            contentDescription = "GhostHID",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Quick send",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tap to reopen dock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSendSheetHeader(
    targetOs: TargetOs,
    targetDevice: BluetoothDevice?,
    onDismiss: () -> Unit,
    historyCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        ) {
            Icon(Icons.Default.Clear, contentDescription = "Close quick send")
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Quick send",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subtitle = if (targetDevice != null) {
                "$historyCount recent sends · ${targetOs.label}"
            } else {
                "$historyCount recent sends · Ghost"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialShapes.ShapeClover4)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (targetDevice != null) {
                OsGlyph(
                    os = targetOs,
                    contentDescription = targetOs.label,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_ghost),
                    contentDescription = "GhostHID",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickSendComposerDock(
    scheme: String,
    analysis: SequenceAnalysis,
    compactPreview: Boolean,
    targetOs: TargetOs = TargetOs.WINDOWS,
    onSchemeChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onSend: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SequencePreview(
                analysis = analysis,
                compact = compactPreview,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            OutlinedTextField(
                value = scheme,
                onValueChange = onSchemeChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (targetOs == TargetOs.MACOS) "cmd+space ->{0.6} type:term" else "win+r ->{0.6} type:cmd",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                ),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (scheme.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear scheme", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSend()
                        focusManager.clearFocus()
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockAction(
                    label = "Paste",
                    icon = Icons.Default.ContentPaste,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onPaste,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onSend,
                    enabled = analysis.canSend,
                    modifier = Modifier
                        .weight(1.45f)
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DockAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
