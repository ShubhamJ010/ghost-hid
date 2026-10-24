package com.sj010.ghid.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj010.ghid.domain.model.Combo
import com.sj010.ghid.domain.model.ComboUsagePolicy
import com.sj010.ghid.domain.model.TargetOs
import com.sj010.ghid.ui.common.os.OsGlyph

/**
 * Dashboard "most used" module.
 *
 * Layout mirrors a streaming app's auto mix: a tinted header band with the
 * section title, a one-line basis caption, and a history glyph on the trailing
 * edge. Below it sits the unchanged ranked list — host icon, combo name, play
 * button — plus a single footer action to leave the module.
 *
 * Only the play button executes a combo, matching the rest of the dashboard.
 */
@Composable
fun MostUsedCombosSection(
    combos: List<Combo>,
    useCounts: Map<String, Int>,
    connectedOs: TargetOs = TargetOs.UNKNOWN,
    onRunCombo: (Combo) -> Unit,
    onViewAll: () -> Unit
) {
    val mostUsed = remember(combos, useCounts, connectedOs) {
        ComboUsagePolicy.mostUsed(combos, useCounts, os = connectedOs)
    }
    val cardShape = RoundedCornerShape(30.dp)

    Surface(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            MostUsedHeader(
                basis = if (connectedOs.isKnown) {
                    "Based on ${connectedOs.label} history"
                } else {
                    "Based on history"
                },
            )

            if (mostUsed.isEmpty()) {
                MostUsedEmptyState(onViewAll = onViewAll)
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mostUsed.forEach { combo ->
                        MostUsedRow(
                            combo = combo,
                            onRun = { onRunCombo(combo) }
                        )
                    }
                }

                MostUsedFooter(onViewAll = onViewAll)
            }
        }
    }
}

/**
 * Tinted header band: heavy uppercase title, one caption line, and a large
 * history glyph on the trailing edge. The glyph sits straight on the band with
 * no container shape. Colors invert the card body (`primary` band with
 * `onPrimary` content) so the module reads as one block.
 */
@Composable
private fun MostUsedHeader(basis: String) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.primary)
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MOST USED",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = scheme.onPrimary
            )
            Text(
                text = basis,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimary.copy(alpha = 0.72f)
            )
        }

        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = scheme.onPrimary,
            modifier = Modifier.size(40.dp)
        )
    }
}

/**
 * One ranked combo row, kept in the original shape: host OS icon, combo title,
 * then a filled play button that executes it. Only the play button runs the
 * combo; the row itself stays inert so a stray tap can never fire a sequence.
 */
@Composable
private fun MostUsedRow(
    combo: Combo,
    onRun: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OsGlyph(
                os = combo.targetOs,
                modifier = Modifier.size(26.dp),
                tint = scheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = combo.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onRun,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run ${combo.name}"
                )
            }
        }
    }
}

/** Footer action: leaves the module for the full combo list. */
@Composable
private fun MostUsedFooter(onViewAll: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewAll)
            .padding(start = 20.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "See all combos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = scheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Zero-history state: same header, a single caption, one browse action. */
@Composable
private fun MostUsedEmptyState(onViewAll: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "No runs yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Run a combo and it lands here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = onViewAll,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            Text("Browse combos")
        }
    }
}

