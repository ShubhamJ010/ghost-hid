package com.sj010.ghid.ui.trackpad

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.ui.theme.MaterialShapes

enum class GestureDirection {
    UP, DOWN, LEFT, RIGHT
}

enum class FingerCount(val count: Int, val label: String) {
    THREE(3, "3 Fingers"),
    FOUR(4, "4 Fingers")
}

enum class GestureShapeType {
    ARCH, SEMI_CIRCLE, FAN, BOOM, CLOVER, SUNNY
}

data class TrackpadGesture(
    val id: String,
    val fingers: FingerCount,
    val direction: GestureDirection,
    val title: String,
    val windowsActionName: String,
    val windowsDescription: String,
    val windowsShortcut: String,
    val windowsCombo: String,
    val macActionName: String,
    val macDescription: String,
    val macShortcut: String,
    val macCombo: String,
    val shapeType: GestureShapeType
) {
    val actionName: String get() = windowsActionName
    val description: String get() = windowsDescription

    fun actionNameFor(os: TargetOs): String = when (os) {
        TargetOs.MACOS -> macActionName
        else -> windowsActionName
    }

    fun descriptionFor(os: TargetOs): String = when (os) {
        TargetOs.MACOS -> macDescription
        else -> windowsDescription
    }

    fun shortcutFor(os: TargetOs): String = when (os) {
        TargetOs.MACOS -> macShortcut
        else -> windowsShortcut
    }

    fun comboFor(os: TargetOs): String = when (os) {
        TargetOs.MACOS -> macCombo
        else -> windowsCombo
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
        windowsActionName = "Task View",
        windowsDescription = "Open Windows Task View to overview running applications",
        windowsShortcut = "Win + Tab",
        windowsCombo = "win+tab",
        macActionName = "Mission Control",
        macDescription = "Open Mission Control to overview open windows and spaces",
        macShortcut = "Ctrl + ↑",
        macCombo = "ctrl+up",
        shapeType = GestureShapeType.ARCH
    )

    val THREE_FINGER_DOWN = TrackpadGesture(
        id = "three_down",
        fingers = FingerCount.THREE,
        direction = GestureDirection.DOWN,
        title = "Swipe Down",
        windowsActionName = "Show Desktop",
        windowsDescription = "Minimize all active windows and reveal the desktop surface",
        windowsShortcut = "Win + D",
        windowsCombo = "win+d",
        macActionName = "Show Desktop",
        macDescription = "Spread open windows to reveal the macOS desktop surface",
        macShortcut = "Cmd + F3",
        macCombo = "cmd+f3",
        shapeType = GestureShapeType.SEMI_CIRCLE
    )

    val THREE_FINGER_LEFT = TrackpadGesture(
        id = "three_left",
        fingers = FingerCount.THREE,
        direction = GestureDirection.LEFT,
        title = "Swipe Left",
        windowsActionName = "Previous App",
        windowsDescription = "Switch back to the previous open window or running app",
        windowsShortcut = "Alt + Shift + Tab",
        windowsCombo = "alt+shift+tab",
        macActionName = "Previous Space",
        macDescription = "Switch to the full-screen space or desktop on the left",
        macShortcut = "Ctrl + ←",
        macCombo = "ctrl+left",
        shapeType = GestureShapeType.FAN
    )

    val THREE_FINGER_RIGHT = TrackpadGesture(
        id = "three_right",
        fingers = FingerCount.THREE,
        direction = GestureDirection.RIGHT,
        title = "Swipe Right",
        windowsActionName = "Next App",
        windowsDescription = "Cycle forward to the next active application or window",
        windowsShortcut = "Alt + Tab",
        windowsCombo = "alt+tab",
        macActionName = "Next Space",
        macDescription = "Switch to the full-screen space or desktop on the right",
        macShortcut = "Ctrl + →",
        macCombo = "ctrl+right",
        shapeType = GestureShapeType.FAN
    )

    val FOUR_FINGER_UP = TrackpadGesture(
        id = "four_up",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.UP,
        title = "Swipe Up",
        windowsActionName = "Maximize / Snap",
        windowsDescription = "Maximize the active window across the screen",
        windowsShortcut = "Win + ↑",
        windowsCombo = "win+up",
        macActionName = "App Exposé",
        macDescription = "View all open windows of the frontmost application",
        macShortcut = "Ctrl + ↓",
        macCombo = "ctrl+down",
        shapeType = GestureShapeType.BOOM
    )

    val FOUR_FINGER_DOWN = TrackpadGesture(
        id = "four_down",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.DOWN,
        title = "Swipe Down",
        windowsActionName = "Minimize All",
        windowsDescription = "Collapse and minimize all open windows to the taskbar",
        windowsShortcut = "Win + M",
        windowsCombo = "win+m",
        macActionName = "Minimize All",
        macDescription = "Minimize all active windows across displays to the Dock",
        macShortcut = "Cmd + Opt + M",
        macCombo = "cmd+opt+m",
        shapeType = GestureShapeType.CLOVER
    )

    val FOUR_FINGER_LEFT = TrackpadGesture(
        id = "four_left",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.LEFT,
        title = "Swipe Left",
        windowsActionName = "Desktop Left",
        windowsDescription = "Switch to the virtual desktop or workspace situated on the left",
        windowsShortcut = "Win + Ctrl + ←",
        windowsCombo = "win+ctrl+left",
        macActionName = "Previous Space",
        macDescription = "Switch to the space or full-screen app situated on the left",
        macShortcut = "Ctrl + ←",
        macCombo = "ctrl+left",
        shapeType = GestureShapeType.SUNNY
    )

    val FOUR_FINGER_RIGHT = TrackpadGesture(
        id = "four_right",
        fingers = FingerCount.FOUR,
        direction = GestureDirection.RIGHT,
        title = "Swipe Right",
        windowsActionName = "Desktop Right",
        windowsDescription = "Switch to the virtual desktop or workspace situated on the right",
        windowsShortcut = "Win + Ctrl + →",
        windowsCombo = "win+ctrl+right",
        macActionName = "Next Space",
        macDescription = "Switch to the space or full-screen app situated on the right",
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
