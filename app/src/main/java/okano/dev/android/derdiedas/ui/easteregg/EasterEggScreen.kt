package okano.dev.android.derdiedas.ui.easteregg

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Easter egg screen with a sweet message
 */
@Composable
fun EasterEggScreen(
    modifier: Modifier = Modifier
) {
    // Infinite pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0033),  // Deep purple
                        Color(0xFF330066),  // Purple
                        Color(0xFF660099)   // Bright purple
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Heart emoji with animation
            Text(
                text = "💖",
                fontSize = 80.sp,
                modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Main message with neon glow effect
            Text(
                text = "Glória,",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF69B4), // Hot pink
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFFFF69B4),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 20f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "I love you so much!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFFFFF),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF00FFFF),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 15f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Decorative text
            Text(
                text = "✨ You're amazing ✨",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFFFD700), // Gold
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFFFFD700),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 10f
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(alpha = alpha)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Signature
            Text(
                text = "Forever yours,\nWilliam",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE6E6FA), // Lavender
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF9370DB),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 12f
                    )
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
