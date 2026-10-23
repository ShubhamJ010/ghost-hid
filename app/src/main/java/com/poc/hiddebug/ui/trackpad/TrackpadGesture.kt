package com.poc.hiddebug.ui.trackpad

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.poc.hiddebug.ui.theme.MaterialShapes

enum class GestureDirection {
    UP, DOWN, LEFT, RIGHT
}

enum class FingerCount(val count: Int, val label: String) {
    THREE(3, "3 Fingers"),
    FOUR(4, "4 Fingers")
}

enum class TargetOs(val label: String) {
    WINDOWS("Windows"),
    MACOS("macOS")
}

enum class GestureShapeType {
    ARCH, SEMI_CIRCLE, FAN, BOOM, CLOVER, SUNNY
}

data class TrackpadGesture(
    val id: String,
    val fingers: FingerCount,
    val direction: GestureDirection,
    val title: String,
    val actionName: String,
    val description: String,
    val windowsShortcut: String,
    val windowsCombo: String,
    val macShortcut: String,
    val macCombo: String,
    val shapeType: GestureShapeType
) {
    fun shortcutFor(os: TargetOs): String = when (os) {
        TargetOs.WINDOWS -> windowsShortcut
        TargetOs.MACOS -> macShortcut
    }

    fun comboFor(os: TargetOs): String = when (os) {
        TargetOs.WINDOWS -> windowsCombo
        TargetOs.MACOS -> macCombo
    }

    fun resolveShape(): Shape = when (shapeType) {
        GestureShapeType.ARCH -> MaterialShapes.ShapeArch
        GestureShapeType.SEMI_CIRCLE -> MaterialShapes.ShapeSemiCircle
        GestureShapeType.FAN -> MaterialShapes.ShapeFan
        GestureShapeType.BOOM -> MaterialShapes.ShapeBoom
        GestureShapeType.CLOVER -> MaterialShapes.ShapeClover4
        GestureShapeType.SUNNY -> MaterialShapes.ShapeSunny
    }

    fun resolveIcon(): ImageVector = when (direction) {
        GestureDirection.UP -> Icons.Default.ArrowUpward
        GestureDirection.DOWN -> Icons.Default.ArrowDownward
        GestureDirection.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        GestureDirection.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
    }
}

object TrackpadGestureRegistry {

    val THREE_FINGER_UP = TrackpadGesture(
        id = "three_up",
        fingers = FingerCount.THREE,
        direction = GestureDirection.UP,
        title = "Swipe Up",
        actionName = "Task View",
        description = "Open Windows Task View or Mission Control to overview running apps",
        windowsShortcut = "Win + Tab",
        windowsCombo = "win+tab",
        macShortcut = "Ctrl + ↑",
        macCombo = "ctrl+up",
        shapeType = GestureShapeType.ARCH
    )

    val THREE_FINGER_DOWN = TrackpadGesture(
        id = "three_down",
        fingers = FingerCount.THREE,
        direction = GestureDirection.DOWN,
        title = "Swipe Down",
        actionName = "Show Desktop",
        description = "Minimize all active windows and reveal the desktop surface",
        windowsShortcut = "Win + D",
        windowsCombo = "win+d",
        macShortcut = "Cmd + F3",
        macCombo = "win+f3",
        shapeType = GestureShapeType.SEMI_CIRCLE
    )

    val THREE_FINGER_LEFT = TrackpadGesture(
        id = "three_left",
        fingers = FingerCount.THREE,
        direction = GestureDirection.LEFT,
        title = "Swipe Left",
        actionName = "Previous App",
        description = "Switch back to the previous open window or running app",
        windowsShortcut = "Alt + Shift + Tab",
        windowsCombo = "alt+shift+tab",
        macShortcut = "Ctrl + ←",
        macCombo = "ctrl+left",
        shapeType = GestureShapeType.FAN
    )

    val THREE_FINGER_RIGHT = TrackpadGesture(
        id = "three_right",
        fingers = FingerCount.THREE,
        direction = GestureDirection.RIGHT,
        title = "Swipe Right",
        actionName = "Next App",
        description = "Cycle forward to the next active application or browser window",
        windowsShortcut = "Alt + Tab",
        windowsCombo = "alt+tab",
        macShortcut = "Ctrl + →",
        macCombo = "ctrl+right",
        shapeType = GestureShapeType.FAN
    )

    val FOUR_FINGER_UP = TrackpadGesture(
        id = "four_up",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.UP,
        title = "Swipe Up",
        actionName = "Maximize / Snap",
        description = "Maximize the active window on Windows or trigger App Exposé on macOS",
        windowsShortcut = "Win + ↑",
        windowsCombo = "win+up",
        macShortcut = "Ctrl + ↓",
        macCombo = "ctrl+down",
        shapeType = GestureShapeType.BOOM
    )

    val FOUR_FINGER_DOWN = TrackpadGesture(
        id = "four_down",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.DOWN,
        title = "Swipe Down",
        actionName = "Minimize All",
        description = "Collapse and minimize all open windows across all displays",
        windowsShortcut = "Win + M",
        windowsCombo = "win+m",
        macShortcut = "Cmd + Opt + M",
        macCombo = "win+alt+m",
        shapeType = GestureShapeType.CLOVER
    )

    val FOUR_FINGER_LEFT = TrackpadGesture(
        id = "four_left",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.LEFT,
        title = "Swipe Left",
        actionName = "Desktop Left",
        description = "Switch to the virtual desktop or workspace situated on the left",
        windowsShortcut = "Win + Ctrl + ←",
        windowsCombo = "win+ctrl+left",
        macShortcut = "Ctrl + ←",
        macCombo = "ctrl+left",
        shapeType = GestureShapeType.SUNNY
    )

    val FOUR_FINGER_RIGHT = TrackpadGesture(
        id = "four_right",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.RIGHT,
        title = "Swipe Right",
        actionName = "Desktop Right",
        description = "Switch to the virtual desktop or workspace situated on the right",
        windowsShortcut = "Win + Ctrl + →",
        windowsCombo = "win+ctrl+right",
        macShortcut = "Ctrl + →",
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

    fun find(fingers: FingerCount, direction: GestureDirection): TrackpadGesture? {
        return ALL_GESTURES.firstOrNull { it.fingers == fingers && it.direction == direction }
    }
}
