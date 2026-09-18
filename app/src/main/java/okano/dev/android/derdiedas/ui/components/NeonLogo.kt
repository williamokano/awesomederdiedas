package okano.dev.android.derdiedas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A neon-style logo showing "der die das" in a German window frame
 */
@Composable
fun NeonLogo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Draw the window frame
        Canvas(modifier = Modifier.size(120.dp)) {
            val windowWidth = size.width * 0.8f
            val windowHeight = size.height * 0.8f
            val left = (size.width - windowWidth) / 2
            val top = (size.height - windowHeight) / 2

            // Neon glow effect - outer glow
            drawRoundRect(
                color = Color(0xFF00FFFF).copy(alpha = 0.3f),
                topLeft = Offset(left - 4f, top - 4f),
                size = Size(windowWidth + 8f, windowHeight + 8f),
                style = Stroke(width = 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
            )

            // Neon glow effect - middle glow
            drawRoundRect(
                color = Color(0xFF00FFFF).copy(alpha = 0.6f),
                topLeft = Offset(left - 2f, top - 2f),
                size = Size(windowWidth + 4f, windowHeight + 4f),
                style = Stroke(width = 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
            )

            // Main window frame (neon cyan)
            drawRoundRect(
                color = Color(0xFF00FFFF),
                topLeft = Offset(left, top),
                size = Size(windowWidth, windowHeight),
                style = Stroke(width = 3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
            )

            // Window cross (dividing the window into 4 panes)
            // Horizontal line
            drawLine(
                color = Color(0xFF00FFFF).copy(alpha = 0.8f),
                start = Offset(left, top + windowHeight / 2),
                end = Offset(left + windowWidth, top + windowHeight / 2),
                strokeWidth = 2f
            )

            // Vertical line
            drawLine(
                color = Color(0xFF00FFFF).copy(alpha = 0.8f),
                start = Offset(left + windowWidth / 2, top),
                end = Offset(left + windowWidth / 2, top + windowHeight),
                strokeWidth = 2f
            )
        }

        // Neon text overlay
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Text with glow effect - shadow layer
            Text(
                text = "der\ndie\ndas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF00FF).copy(alpha = 0.8f),
                lineHeight = 22.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFFFF00FF),
                        offset = Offset(0f, 0f),
                        blurRadius = 15f
                    )
                )
            )
        }
    }
}
