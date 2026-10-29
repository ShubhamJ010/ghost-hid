package com.sj010.ghid.ui.interactive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.R
import com.sj010.ghid.domain.model.ConnectionStatus
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.theme.ExpressiveShapes
import kotlinx.coroutines.delay

/**
 * Interactive mode: the phone's own keyboard becomes the Bluetooth keyboard of
 * the connected device.
 *
 * Every soft-keyboard press is forwarded immediately. The keyboard island
 * chains sticky modifiers and quick keys, while a separate preview shows
 * armed modifiers and recently sent keys. The key drawer contains modifier,
 * navigation, media and function keys.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveModeScreen(service: HidService?, onExit: () -> Unit) {
    val connectionState by service?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionStatus.DISCONNECTED) }
    val isLinked = connectionState == ConnectionStatus.CONNECTED
    val detectedOs by service?.effectiveTargetOs?.collectAsState()
        ?: remember { mutableStateOf(TargetOs.WINDOWS) }

    var heldModifiers by remember { mutableStateOf(emptySet<InteractiveModifier>()) }
    var history by remember { mutableStateOf(emptyList<String>()) }
    var isKeyDrawerOpen by remember { mutableStateOf(false) }
    // The capture field is anchored with an invisible newline so a backspace is
    // always a real deletion; without it the empty field would swallow it.
    var captureValue by remember { mutableStateOf(sentinelValue()) }

    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val captureFocus = remember { FocusRequester() }
    val focusCaptureField: () -> Unit = {
        captureFocus.requestFocus()
        keyboardController?.show()
    }

    fun log(keyLabel: String, modifiers: Set<InteractiveModifier>) {
        val prefix = InteractiveKeys.modifierLabel(modifiers, detectedOs)
        val entry = if (prefix == null) keyLabel else "$prefix + $keyLabel"
        history = (listOf(entry) + history).take(8)
        heldModifiers = emptySet()
    }

    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyDrawerOpen, imeVisible) {
        if (isKeyDrawerOpen) {
            keyboardController?.hide()
        } else if (!imeVisible) {
            delay(420)
            captureFocus.requestFocus()
            keyboardController?.show()
        }
    }

    val sendCatalogKey: (InteractiveKey) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        InteractiveKeys.send(service, key, heldModifiers)
        log(key.label, heldModifiers)
    }

    InteractiveKeyShade(
        targetOs = detectedOs,
        heldModifiers = heldModifiers,
        onKey = sendCatalogKey,
        onToggleModifier = { modifier ->
            heldModifiers = if (modifier in heldModifiers) {
                heldModifiers - modifier
            } else {
                heldModifiers + modifier
            }
        },
        onOpenChanged = { isKeyDrawerOpen = it }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Center stage: the ghost floating above the phone keyboard.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ghost_keyboard_mode),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "Interactive mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLinked) {
                                "Every key you type is sent to the device."
                            } else {
                                "Not connected. Connect to a device to type through."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            StatusPill(isLinked = isLinked)
                        }
                        InteractivePreviewChips(
                            heldModifiers = heldModifiers,
                            history = history,
                            targetOs = detectedOs
                        )
                    }
                }

                // Attached deck sitting directly on top of the Android keyboard.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 18.dp
                    ),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = focusCaptureField
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            InteractiveKeys.modifiers.forEach { modifier ->
                                ModifierKeyChip(
                                    modifier = Modifier.weight(1f),
                                    label = modifier.labelFor(detectedOs),
                                    active = modifier in heldModifiers,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        heldModifiers = if (modifier in heldModifiers) {
                                            heldModifiers - modifier
                                        } else {
                                            heldModifiers + modifier
                                        }
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickKey(
                                modifier = Modifier.weight(1f),
                                label = "Esc",
                                onClick = {
                                    sendCatalogKey(
                                        InteractiveKeys.catalog.first { it.token == "esc" }
                                    )
                                }
                            )
                            ExitInteractiveKey(
                                modifier = Modifier.weight(1.35f),
                                onClick = {
                                    keyboardController?.hide()
                                    onExit()
                                }
                            )
                            QuickKey(
                                modifier = Modifier.weight(1f),
                                label = "",
                                contentDescription = "Play/Pause",
                                icon = Icons.Default.PlayArrow,
                                onClick = {
                                    sendCatalogKey(
                                        InteractiveKeys.catalog.first { it.token == "playpause" }
                                    )
                                }
                            )
                        }

                        Text(
                            text = "Modifiers stay armed until the next key.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Invisible capture field: the system IME stays attached to it while
            // every diff is forwarded over HID.
            BasicTextField(
                value = captureValue,
                onValueChange = { newValue ->
                    val oldText = captureValue.text
                    val newText = newValue.text
                    when {
                        newText.startsWith(oldText) -> {
                            newText.substring(oldText.length).forEach { char ->
                                if (InteractiveKeys.sendChar(service, char, heldModifiers)) {
                                    log(labelForChar(char), heldModifiers)
                                }
                            }
                        }

                        oldText.startsWith(newText) -> {
                            repeat(oldText.length - newText.length) {
                                InteractiveKeys.sendBackspace(service)
                                history = (listOf("⌫") + history).take(8)
                            }
                        }

                        else -> {
                            // The IME replaced the whole field instead of editing it.
                            newText.removePrefix(CAPTURE_SENTINEL).forEach { char ->
                                if (InteractiveKeys.sendChar(service, char, heldModifiers)) {
                                    log(labelForChar(char), heldModifiers)
                                }
                            }
                        }
                    }
                    // Re-anchor at the end so every keystroke is a fresh append.
                    captureValue = sentinelValue()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(captureFocus),
                textStyle = MaterialTheme.typography.bodySmall,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.None
                )
            )
        }
    }
}

private const val CAPTURE_SENTINEL = "\n"

private fun sentinelValue() = TextFieldValue(
    text = CAPTURE_SENTINEL,
    selection = TextRange(CAPTURE_SENTINEL.length)
)

private fun labelForChar(char: Char): String = when (char) {
    ' ' -> "Space"
    '\n', '\r' -> "Enter"
    '\t' -> "Tab"
    else -> char.toString()
}

@Composable
private fun StatusPill(isLinked: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isLinked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = if (isLinked) "CONNECTED" else "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = if (isLinked) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractivePreviewChips(
    heldModifiers: Set<InteractiveModifier>,
    history: List<String>,
    targetOs: TargetOs
) {
    if (heldModifiers.isEmpty() && history.isEmpty()) {
        Text(
            text = "Press a modifier to chain it, then any key is sent straight to the device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        heldModifiers.forEach { modifier ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = modifier.labelFor(targetOs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        history.forEach { entry ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = entry,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ModifierKeyChip(
    modifier: Modifier,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 8.dp,
            bottomEnd = 14.dp,
            bottomStart = 8.dp
        ),
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            1.dp,
            if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickKey(
    modifier: Modifier,
    label: String,
    contentDescription: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 14.dp,
            bottomEnd = 8.dp,
            bottomStart = 14.dp
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (icon == null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExitInteractiveKey(
    modifier: Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 8.dp,
            bottomEnd = 14.dp,
            bottomStart = 8.dp
        ),
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Exit",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}
