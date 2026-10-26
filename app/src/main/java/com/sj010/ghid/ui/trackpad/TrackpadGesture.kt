package com.sj010.ghid.ui.trackpad

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.ui.theme.ExpressiveShapes

/** Physical direction of a multi-finger swipe on the virtual trackpad. */
enum class GestureDirection {
    UP, DOWN, LEFT, RIGHT
}

/** Finger count a gesture requires; the label is UI copy for filter chips. */
enum class FingerCount(val count: Int, val label: String) {
    THREE(3, "3 fingers"),
    FOUR(4, "4 fingers")
}

/** Which Material 3 Expressive shape renders a gesture's badge. */
enum class GestureShapeType {
    ARCH, SEMI_CIRCLE, FAN, BOOM, CLOVER, SUNNY
}

/**
 * One 3- or 4-finger trackpad swipe and the host action it performs.
 *
 * Only the host-facing action label and the combo that gets sent are modelled.
 * Keyboard shortcut notation is intentionally absent: the gallery shows the swipe
 * the user performs, never the keys an implementation uses underneath.
 *
 * @property title the gesture itself, e.g. "Swipe Up" — used as the card headline.
 * @property windowsActionName / [macActionName] short action labels for each host OS.
 * @property windowsCombo / [macCombo] combo strings sent through the HID service.
 * @property fingers required finger count, rendered as dots on the card.
 * @property direction swipe direction, drives the badge arrow.
 */
data class TrackpadGesture(
    val id: String,
    val fingers: FingerCount,
    val direction: GestureDirection,
    val title: String,
    val windowsActionName: String,
    val windowsCombo: String,
    val macActionName: String,
    val macCombo: String,
    val shapeType: GestureShapeType
) {
    /** Action label for the active host OS. */
    fun actionNameFor(os: TargetOs): String = when (os) {
        TargetOs.MACOS -> macActionName
        else -> windowsActionName
    }

    /** Combo string to send to the active host OS. */
    fun comboFor(os: TargetOs): String = when (os) {
        TargetOs.MACOS -> macCombo
        else -> windowsCombo
    }

    /** Expressive container shape for this gesture's badge. */
    fun resolveShape(): Shape = when (shapeType) {
        GestureShapeType.ARCH -> ExpressiveShapes.Arch
        GestureShapeType.SEMI_CIRCLE -> ExpressiveShapes.SemiCircle
        GestureShapeType.FAN -> ExpressiveShapes.Fan
        GestureShapeType.BOOM -> ExpressiveShapes.Boom
        GestureShapeType.CLOVER -> ExpressiveShapes.Clover4Leaf
        GestureShapeType.SUNNY -> ExpressiveShapes.Sunny
    }

    /** Direction arrow drawn inside the badge. */
    fun resolveIcon(): ImageVector = when (direction) {
        GestureDirection.UP -> Icons.Default.ArrowUpward
        GestureDirection.DOWN -> Icons.Default.ArrowDownward
        GestureDirection.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        GestureDirection.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
    }
}

/**
 * Static catalog of every recognized swipe, with per-host combo mappings.
 * Keep ids stable: the gesture HUD keys transient animation state off them.
 */
object TrackpadGestureRegistry {

    val THREE_FINGER_UP = TrackpadGesture(
        id = "three_up",
        fingers = FingerCount.THREE,
        direction = GestureDirection.UP,
        title = "Swipe Up",
        windowsActionName = "Task View",
        windowsCombo = "win+tab",
        macActionName = "Mission Control",
        macCombo = "ctrl+up",
        shapeType = GestureShapeType.ARCH
    )

    val THREE_FINGER_DOWN = TrackpadGesture(
        id = "three_down",
        fingers = FingerCount.THREE,
        direction = GestureDirection.DOWN,
        title = "Swipe Down",
        windowsActionName = "Show Desktop",
        windowsCombo = "win+d",
        macActionName = "Show Desktop",
        macCombo = "cmd+f3",
        shapeType = GestureShapeType.SEMI_CIRCLE
    )

    val THREE_FINGER_LEFT = TrackpadGesture(
        id = "three_left",
        fingers = FingerCount.THREE,
        direction = GestureDirection.LEFT,
        title = "Swipe Left",
        windowsActionName = "Previous App",
        windowsCombo = "alt+shift+tab",
        macActionName = "Previous Space",
        macCombo = "ctrl+left",
        shapeType = GestureShapeType.FAN
    )

    val THREE_FINGER_RIGHT = TrackpadGesture(
        id = "three_right",
        fingers = FingerCount.THREE,
        direction = GestureDirection.RIGHT,
        title = "Swipe Right",
        windowsActionName = "Next App",
        windowsCombo = "alt+tab",
        macActionName = "Next Space",
        macCombo = "ctrl+right",
        shapeType = GestureShapeType.FAN
    )

    val FOUR_FINGER_UP = TrackpadGesture(
        id = "four_up",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.UP,
        title = "Swipe Up",
        windowsActionName = "Maximize",
        windowsCombo = "win+up",
        macActionName = "App Exposé",
        macCombo = "ctrl+down",
        shapeType = GestureShapeType.BOOM
    )

    val FOUR_FINGER_DOWN = TrackpadGesture(
        id = "four_down",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.DOWN,
        title = "Swipe Down",
        windowsActionName = "Minimize All",
        windowsCombo = "win+m",
        macActionName = "Minimize All",
        macCombo = "cmd+opt+m",
        shapeType = GestureShapeType.CLOVER
    )

    val FOUR_FINGER_LEFT = TrackpadGesture(
        id = "four_left",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.LEFT,
        title = "Swipe Left",
        windowsActionName = "Desktop Left",
        windowsCombo = "win+ctrl+left",
        macActionName = "Previous Space",
        macCombo = "ctrl+left",
        shapeType = GestureShapeType.SUNNY
    )

    val FOUR_FINGER_RIGHT = TrackpadGesture(
        id = "four_right",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.RIGHT,
        title = "Swipe Right",
        windowsActionName = "Desktop Right",
        windowsCombo = "win+ctrl+right",
        macActionName = "Next Space",
        macCombo = "ctrl+right",
        shapeType = GestureShapeType.SUNNY
    )

    val ALL_GESTURES = listOf(
        THREE_FINGER_UP,
        THREE_FINGER_DOWN,
        THREE_FINGER_LEFT,
        THREE_FINGER_RIGHT,
        FOUR_FINGER_UP,
        FOUR_FINGER_DOWN,
        FOUR_FINGER_LEFT,
        FOUR_FINGER_RIGHT
    )

    /** Resolves the gesture for an incoming multi-finger swipe, or null if unmapped. */
    fun find(fingers: FingerCount, direction: GestureDirection): TrackpadGesture? {
        return ALL_GESTURES.firstOrNull { it.fingers == fingers && it.direction == direction }
    }
}
