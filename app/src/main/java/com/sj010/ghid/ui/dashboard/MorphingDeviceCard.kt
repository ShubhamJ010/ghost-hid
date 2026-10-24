package com.sj010.ghid.ui.dashboard

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.sj010.ghid.domain.model.ConnectionStatus
import kotlin.math.max

/**
 * One device card, two poses, driven by the shade progress.
 *
 * Progress 0 is the compact dock: device identity, state badge, and an icon action.
 * Progress 1 is the expanded panel: target details and the same icon action.
 * Opening and closing share this progress, so the card regresses back into the dock.
 */
@Composable
internal fun MorphingDeviceCard(
    progress: Float,
    connectionState: ConnectionStatus,
    targetDevice: BluetoothDevice?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSwitchDevice: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = progress.coerceIn(0f, 1f)
    val cardInteraction = remember { MutableInteractionSource() }
    val status = deviceStatusVisual(connectionState)
    val detailsFraction = ((fraction - 0.18f) / 0.82f).coerceIn(0f, 1f)

    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = { if (fraction < 0.5f) onOpen() else onDismiss() }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) { constraints ->
        if (constraints.maxWidth <= 0 || constraints.maxHeight <= 0) {
            return@SubcomposeLayout layout(constraints.maxWidth, constraints.maxHeight) {}
        }

        val gap = 12.dp.roundToPx()
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val wrapButton = subcompose("connect-measure") {
            ExpressiveConnectButton(
                connectionState = connectionState,
                hasTarget = targetDevice != null,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onNeedsTarget = onSwitchDevice,
                modifier = Modifier.height(40.dp)
            )
        }.first().measure(loose)

        val buttonWidth = lerp(wrapButton.width, constraints.maxWidth, fraction)
        val buttonHeight = lerp(wrapButton.height, 56.dp.roundToPx(), fraction)
        val button = subcompose("connect") {
            ExpressiveConnectButton(
                connectionState = connectionState,
                hasTarget = targetDevice != null,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onNeedsTarget = onSwitchDevice,
                modifier = Modifier.fillMaxSize()
            )
        }.first().measure(Constraints.fixed(buttonWidth, buttonHeight))

        val reservedForButton = lerp(wrapButton.width + gap, 0, fraction)
        val identityWidth = (constraints.maxWidth - reservedForButton).coerceAtLeast(0)
        val identity = subcompose("identity") {
            DeviceIdentityRow(
                fraction = fraction,
                detailsFraction = detailsFraction,
                statusIcon = status.icon,
                iconContainer = status.iconContainer,
                iconContent = status.iconContent,
                targetDevice = targetDevice,
                onSwitchDevice = onSwitchDevice
            )
        }.first().measure(
            Constraints(
                minWidth = identityWidth,
                maxWidth = identityWidth,
                maxHeight = constraints.maxHeight
            )
        )

        val handle = subcompose("handle") {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f))
            )
        }.first().measure(Constraints.fixed(44.dp.roundToPx(), 4.dp.roundToPx()))

        val rowHeight = max(identity.height, button.height)
        val compactTopInset = (
            (constraints.maxHeight - rowHeight).coerceAtLeast(0) / 2
        ).coerceAtMost(12.dp.roundToPx())
        val expandedTopInset = handle.height + 24.dp.roundToPx()
        val identityY = lerp(compactTopInset, expandedTopInset, fraction)
        val compactButtonY = compactTopInset + (rowHeight - button.height) / 2
        val expandedButtonY = (identityY + identity.height + 16.dp.roundToPx())
            .coerceAtMost((constraints.maxHeight - button.height).coerceAtLeast(0))
        val buttonY = lerp(compactButtonY, expandedButtonY, fraction)
        val buttonX = lerp(constraints.maxWidth - button.width, 0, fraction)
        val compactHandleY = (compactTopInset + rowHeight + 21.dp.roundToPx())
            .coerceAtMost((constraints.maxHeight - handle.height).coerceAtLeast(0))
        val expandedHandleY = 15.dp.roundToPx()
        val handleY = lerp(compactHandleY, expandedHandleY, fraction)
        val handleX = (constraints.maxWidth - handle.width) / 2

        layout(constraints.maxWidth, constraints.maxHeight) {
            identity.placeRelative(0, identityY)
            button.placeRelative(buttonX, buttonY)
            handle.placeRelative(handleX, handleY)
        }
    }
}

@Composable
private fun DeviceIdentityRow(
    fraction: Float,
    detailsFraction: Float,
    statusIcon: ImageVector,
    iconContainer: Color,
    iconContent: Color,
    targetDevice: BluetoothDevice?,
    onSwitchDevice: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val hostName = targetDevice?.name ?: "Select a device"
    val expandedSubtitle = targetDevice?.address ?: "Tap Switch to pick host PC"
    val expandedText = detailsFraction
    val wellColor = iconContainer
    val corner = lerp(23.dp, 14.dp, fraction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                scheme.surfaceContainerHighest.copy(alpha = fraction),
                RoundedCornerShape(20.dp)
            )
            .padding(
                horizontal = lerp(0.dp, 14.dp, fraction),
                vertical = lerp(0.dp, 12.dp, fraction)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
                    .background(wellColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = iconContent,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { alpha = (1f - fraction * 1.4f).coerceIn(0f, 1f) }
                )
                Icon(
                    imageVector = Icons.Outlined.Laptop,
                    contentDescription = null,
                    tint = iconContent,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { alpha = detailsFraction }
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box {
                Text(
                    text = hostName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    // The collapsed Android shade gives the identity row less
                    // room beside the disconnect action. Wrap the host name so
                    // the paired device remains identifiable at a glance.
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.graphicsLayer { alpha = 1f }
                )
            }
            Box(
                modifier = Modifier
                    .height(lerp(0.dp, 20.dp, expandedText))
                    .clipToBounds()
            ) {
                Text(
                    text = expandedSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    fontFamily = if (targetDevice != null) FontFamily.Monospace else FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = expandedText }
                )
            }
        }
        Spacer(Modifier.width(lerp(0.dp, 8.dp, fraction)))
        FilledTonalButton(
            onClick = onSwitchDevice,
            enabled = fraction > 0.72f,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
                .revealWidth(fraction)
                .graphicsLayer { alpha = detailsFraction }
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Switch",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * The icon action uses Material color roles for the current connection state.
 */
@Composable
private fun ExpressiveConnectButton(
    connectionState: ConnectionStatus,
    hasTarget: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNeedsTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 16.dp else 100.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "connectShape"
    )
    val isConnected = connectionState == ConnectionStatus.CONNECTED
    val isBusy = connectionState == ConnectionStatus.CONNECTING ||
        connectionState == ConnectionStatus.DISCONNECTING
    val label = when (connectionState) {
        ConnectionStatus.CONNECTED -> "Disconnect"
        ConnectionStatus.CONNECTING -> "Connecting…"
        ConnectionStatus.DISCONNECTING -> "Disconnecting…"
        ConnectionStatus.DISCONNECTED, ConnectionStatus.UNKNOWN -> "Connect"
    }
    val disconnectButtonColors = if (scheme.surface.luminance() < 0.5f) {
        scheme.errorContainer to scheme.onErrorContainer
    } else {
        scheme.error to scheme.onError
    }
    val buttonColors = when (connectionState) {
        ConnectionStatus.CONNECTED -> disconnectButtonColors
        ConnectionStatus.CONNECTING, ConnectionStatus.DISCONNECTING ->
            scheme.tertiaryContainer to scheme.onTertiaryContainer
        ConnectionStatus.DISCONNECTED, ConnectionStatus.UNKNOWN ->
            scheme.primary to scheme.onPrimary
    }
    val container = buttonColors.first
    val content = buttonColors.second
    val actionDescription = when {
        isBusy && connectionState == ConnectionStatus.CONNECTING -> "Connecting to device"
        isBusy -> "Disconnecting from device"
        !hasTarget -> "Select a device"
        isConnected -> "Disconnect from device"
        else -> "Connect to device"
    }
    val icon = when {
        isBusy && connectionState == ConnectionStatus.CONNECTING -> Icons.Default.SyncAlt
        isConnected || connectionState == ConnectionStatus.DISCONNECTING -> Icons.Default.BluetoothDisabled
        else -> Icons.Default.Bluetooth
    }

    Button(
        onClick = {
            when {
                !hasTarget -> onNeedsTarget()
                isConnected || connectionState == ConnectionStatus.DISCONNECTING -> onDisconnect()
                else -> onConnect()
            }
        },
        enabled = connectionState != ConnectionStatus.DISCONNECTING,
        interactionSource = interaction,
        modifier = modifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            .semantics { contentDescription = actionDescription },
        shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.38f),
            disabledContentColor = content.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = content
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Modifier.revealWidth(progress: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val width = (placeable.width * progress).toInt().coerceIn(0, placeable.width)
    layout(width, placeable.height) {
        placeable.placeRelative(width - placeable.width, 0)
    }
}

private data class DeviceStatusVisual(
    val iconContainer: Color,
    val iconContent: Color,
    val icon: ImageVector
)

@Composable
private fun deviceStatusVisual(status: ConnectionStatus): DeviceStatusVisual {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        ConnectionStatus.CONNECTED -> DeviceStatusVisual(
            iconContainer = scheme.secondaryContainer,
            iconContent = scheme.onSecondaryContainer,
            icon = Icons.Default.BluetoothConnected
        )
        ConnectionStatus.CONNECTING -> DeviceStatusVisual(
            iconContainer = scheme.tertiaryContainer,
            iconContent = scheme.onTertiaryContainer,
            icon = Icons.Default.SyncAlt
        )
        ConnectionStatus.DISCONNECTING -> DeviceStatusVisual(
            iconContainer = scheme.errorContainer,
            iconContent = scheme.onErrorContainer,
            icon = Icons.Default.BluetoothDisabled
        )
        ConnectionStatus.DISCONNECTED, ConnectionStatus.UNKNOWN -> DeviceStatusVisual(
            iconContainer = scheme.errorContainer,
            iconContent = scheme.onErrorContainer,
            icon = Icons.Default.BluetoothDisabled
        )
    }
}
