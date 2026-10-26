package com.sj010.ghid.ui.streamdeck

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.ComboOsPolicy
import com.sj010.ghid.domain.model.Payload
import com.sj010.ghid.domain.model.PinnedFavoritesPolicy
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.service.HidService
import com.sj010.ghid.ui.common.shade.PullDownShade
import com.sj010.ghid.ui.dashboard.FavoritePickerSheet

/**
 * Full-screen landscape "Ghost Deck".
 *
 * The five dashboard pin slots become large, scattered M3 Expressive shapes.
 * Pulling from the notification edge reveals every payload as a shaped pill,
 * ordered by dispatch count.
 */
@Composable
fun StreamDeckScreen(
    service: HidService?,
    modifier: Modifier = Modifier
) {
    val app = GhostHidApp.instance
    val allCombos by app.comboRepository.combos.collectAsState()
    val pins by app.pinnedFavoritesRepository.pins.collectAsState()
    val payloads by app.payloadRepository.payloads.collectAsState()
    val payloadUseCounts by app.payloadUsageRepository.useCounts.collectAsState()
    val targetOs = service?.effectiveTargetOs?.collectAsState()?.value ?: TargetOs.UNKNOWN
    val haptic = LocalHapticFeedback.current

    val pinnedCombos = remember(pins, allCombos, targetOs) {
        PinnedFavoritesPolicy.resolve(pins, allCombos)
            .filterValues { ComboOsPolicy.matches(it, targetOs) }
    }
    val favorites = remember(allCombos, targetOs) {
        ComboOsPolicy.favorites(allCombos, targetOs)
    }
    val slots = remember { streamDeckSlots() }
    val shapeNames = remember(slots) { slots.associate { it.slotId to it.shapeName } }
    var pickerSlot by remember { mutableStateOf<StreamDeckSlot?>(null) }

    ImmersiveDeck()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        PullDownShade(
            modifier = Modifier.fillMaxSize(),
            dock = { scope ->
                StreamDeckDock(
                    progress = scope.progress,
                    payloadCount = payloads.size,
                    onDismiss = { scope.dismiss() },
                    onOpen = { scope.open() },
                    dragHandleModifier = scope.dragHandleModifier
                )
            },
            shade = {
                StreamDeckPayloadShade(
                    payloads = payloads,
                    useCounts = payloadUseCounts,
                    targetOs = targetOs,
                    onSend = { payload -> dispatchPayload(service, payload) }
                )
            },
            dockHeight = 56.dp,
            openDockHeight = 56.dp,
            drawBorder = false,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            StreamDeckCollage(
                pinnedBySlot = pinnedCombos,
                slots = slots,
                onRunCombo = { combo ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    service?.executeCombo(combo.effectiveSequence)
                },
                onOpenPicker = { slot ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pickerSlot = slot
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    pickerSlot?.let { slot ->
        FavoritePickerSheet(
            slotId = slot.slotId,
            slotLabel = slot.emptyLabel,
            slotShape = slot.shape,
            slotContainerColor = slot.containerColor,
            slotContentColor = slot.contentColor,
            favorites = favorites,
            pinnedBySlot = pinnedCombos,
            shapeNames = shapeNames,
            onSelect = { combo ->
                app.pinnedFavoritesRepository.pin(slot.slotId, combo.name)
                pickerSlot = null
            },
            onBrowseCombos = null,
            onDismiss = { pickerSlot = null }
        )
    }
}

/**
 * Immersive landscape mode: the deck owns the whole display while the phone is
 * sideways, and system bars return when the user rotates back to portrait.
 */
@Composable
private fun ImmersiveDeck() {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context.findActivity()
            ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val previousBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}

private fun dispatchPayload(service: HidService?, payload: Payload) {
    val sender = service ?: return
    val value = payload.value?.trim().orEmpty()
    if (value.isEmpty()) return

    val explicitRaw = value.startsWith("raw:", ignoreCase = true)
    if (payload.isRaw || explicitRaw) {
        val hex = if (explicitRaw) value.substring(4).trim() else value
        val cleanHex = hex.replace(" ", "")
        val validHex = cleanHex.length in listOf(2, 4, 8, 16) &&
            cleanHex.all { it in "0123456789abcdefABCDEF" }
        if (!validHex) return

        GhostHidApp.instance.payloadUsageRepository.recordUse(payload.name)
        val bytes = ByteArray(cleanHex.length / 2) { index ->
            cleanHex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        sender.sendRawReport(bytes)
    } else {
        GhostHidApp.instance.payloadUsageRepository.recordUse(payload.name)
        sender.typeText(value)
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
