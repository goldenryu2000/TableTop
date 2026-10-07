package dev.tabletop.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Small caption above a tabular number, used in game top bars. */
@Composable
fun StatLabel(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.6f))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = color,
        )
    }
}

/** Icon over a short label; the main control style in game bottom bars. */
@Composable
fun ToolbarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .widthIn(min = 64.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
