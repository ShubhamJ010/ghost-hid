package com.sj010.ghid.ui.interactive

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.graphics.vector.ImageVector
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.domain.parser.KeyTokens
import com.sj010.ghid.domain.pointer.MouseButton
import com.sj010.ghid.service.HidService

/**
 * Sticky modifiers for interactive mode. Several can be chained at once and the
 * whole set is OR'd into a single HID modifier byte for the next key press.
 */
enum class InteractiveModifier(val label: String, val bit: Int) {
    CTRL("Ctrl", 0x01),
    SHIFT("Shift", 0x02),
    ALT("Alt", 0x04),
    GUI("Win", 0x08);

    fun labelFor(targetOs: TargetOs): String =
        if (this == GUI && targetOs == TargetOs.MACOS) "Cmd" else label
}

enum class InteractiveKeyCategory(val title: String, val icon: ImageVector) {
    NAVIGATION("Navigation", Icons.Default.Navigation),
    MOUSE("Mouse", Icons.Default.Mouse),
    MEDIA("Media", Icons.AutoMirrored.Filled.VolumeUp),
    FUNCTION("Function", Icons.Default.Functions)
}

/**
 * One tappable key in the interactive drawer. [token] is a parser key token, so
 * the drawer and the sequence parser can never disagree on a keycode.
 * Consumer-control media keys carry their HID usage instead.
 */
data class InteractiveKey(
    val label: String,
    val token: String,
    val category: InteractiveKeyCategory,
    val consumerUsage: Int? = null,
    val mouseButton: MouseButton? = null,
    /** Compact label for the narrow mapper row, where [label] would clip. */
    val shortLabel: String? = null,
    /** Drawn instead of the label wherever this key is assigned. */
    val icon: ImageVector? = null
)

object InteractiveKeys {

    val modifiers = InteractiveModifier.entries

    private val navigationKeys = listOf(
        InteractiveKey("Up", "up", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Down", "down", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Left", "left", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Right", "right", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Home", "home", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("End", "end", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("PgUp", "pageup", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("PgDn", "pagedown", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Insert", "insert", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Delete", "delete", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Backspace", "backspace", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Enter", "enter", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Tab", "tab", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Esc", "esc", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Space", "space", InteractiveKeyCategory.NAVIGATION),
        InteractiveKey("Menu", "menu", InteractiveKeyCategory.NAVIGATION)
    )

    private val mouseKeys = listOf(
        InteractiveKey("Left Click", "lclick", InteractiveKeyCategory.MOUSE, mouseButton = MouseButton.LEFT, shortLabel = "L Click"),
        InteractiveKey("Right Click", "rclick", InteractiveKeyCategory.MOUSE, mouseButton = MouseButton.RIGHT, shortLabel = "R Click"),
        InteractiveKey("Middle Click", "mclick", InteractiveKeyCategory.MOUSE, mouseButton = MouseButton.MIDDLE, shortLabel = "M Click"),
        InteractiveKey("Back", "mouseback", InteractiveKeyCategory.MOUSE, mouseButton = MouseButton.BACK, shortLabel = "Back"),
        InteractiveKey("Forward", "mouseforward", InteractiveKeyCategory.MOUSE, mouseButton = MouseButton.FORWARD, shortLabel = "Fwd")
    )

    private val mediaKeys = listOf(
        InteractiveKey("Volume Up", "volumeup", InteractiveKeyCategory.MEDIA, consumerUsage = 0xE9, icon = Icons.AutoMirrored.Filled.VolumeUp),
        InteractiveKey("Volume Down", "volumedown", InteractiveKeyCategory.MEDIA, consumerUsage = 0xEA, icon = Icons.AutoMirrored.Filled.VolumeDown),
        InteractiveKey("Mute", "mute", InteractiveKeyCategory.MEDIA, consumerUsage = 0xE2, icon = Icons.AutoMirrored.Filled.VolumeMute),
        InteractiveKey("Play/Pause", "playpause", InteractiveKeyCategory.MEDIA, consumerUsage = 0xCD, icon = Icons.Filled.PlayArrow),
        InteractiveKey("Next Track", "nexttrack", InteractiveKeyCategory.MEDIA, consumerUsage = 0xB5, icon = Icons.Filled.SkipNext),
        InteractiveKey("Previous Track", "previoustrack", InteractiveKeyCategory.MEDIA, consumerUsage = 0xB6, icon = Icons.Filled.SkipPrevious)
    )

    private val functionKeys = buildList {
        (1..12).forEach { n ->
            add(InteractiveKey("F$n", "f$n", InteractiveKeyCategory.FUNCTION))
        }
        add(InteractiveKey("PrtSc", "printscreen", InteractiveKeyCategory.FUNCTION))
        add(InteractiveKey("Scroll Lock", "scrolllock", InteractiveKeyCategory.FUNCTION))
        add(InteractiveKey("Pause", "pause", InteractiveKeyCategory.FUNCTION))
        add(InteractiveKey("Caps Lock", "capslock", InteractiveKeyCategory.FUNCTION))
        add(InteractiveKey("Num Lock", "numlock", InteractiveKeyCategory.FUNCTION))
    }

    val catalog: List<InteractiveKey> = navigationKeys + mouseKeys + mediaKeys + functionKeys

    fun findByToken(token: String): InteractiveKey? {
        if (token.isBlank()) return null
        return catalog.firstOrNull { it.token.equals(token, ignoreCase = true) }
    }

    fun keysIn(category: InteractiveKeyCategory): List<InteractiveKey> =
        catalog.filter { it.category == category }

    fun modifierByte(modifiers: Set<InteractiveModifier>): Byte =
        modifiers.fold(0) { acc, modifier -> acc or modifier.bit }.toByte()

    fun modifierLabel(modifiers: Set<InteractiveModifier>, targetOs: TargetOs): String? =
        if (modifiers.isEmpty()) {
            null
        } else {
            modifiers.joinToString(" + ") { it.labelFor(targetOs) }
        }

    /** Sends one catalog key with the chained modifiers applied. */
    fun send(service: HidService?, key: InteractiveKey, modifiers: Set<InteractiveModifier>) {
        val svc = service ?: return
        key.mouseButton?.let { button ->
            svc.sendMouseButton(button)
            return
        }
        key.consumerUsage?.let { usage ->
            svc.sendConsumerKey(usage)
            return
        }
        val (chordMod, keyCode) = KeyTokens.parseChord(key.token)
        if (keyCode != 0.toByte()) {
            val combinedMod = (modifierByte(modifiers).toInt() or chordMod.toInt()).toByte()
            svc.sendKey(combinedMod, keyCode)
        }
    }

    /** Sends one soft-keyboard character, folding in the chained modifiers. */
    fun sendChar(service: HidService?, char: Char, modifiers: Set<InteractiveModifier>) {
        val svc = service ?: return
        val (charModifier, keyCode) = KeyTokens.keyForChar(char)
        if (keyCode == 0.toByte()) return
        val combined = (charModifier.toInt() or modifierByte(modifiers).toInt()).toByte()
        svc.sendKey(combined, keyCode)
    }

    fun sendBackspace(service: HidService?) {
        service?.sendKey(0, 0x2A)
    }

}
