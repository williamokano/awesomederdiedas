package okano.dev.android.derdiedas.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.ui.resources.StringResources
import java.util.concurrent.TimeUnit

/**
 * Screen showing game results after completion
 */
@Composable
fun GameResultsScreen(
    correctAnswers: Int,
    wrongAnswers: Int,
    durationMillis: Long,
    cardsPerMinute: Float,
    language: Language,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCards = correctAnswers + wrongAnswers
    val accuracyPercentage = if (totalCards > 0) {
        ((correctAnswers.toFloat() / totalCards) * 100).toInt()
    } else {
        0
    }

    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = when (language) {
                Language.ENGLISH -> "Game Complete!"
                Language.PORTUGUESE -> "Jogo Completo!"
            },
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Results Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Accuracy with big visual
                Text(
                    text = "$accuracyPercentage%",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        accuracyPercentage >= 80 -> Color(0xFF4CAF50)
                        accuracyPercentage >= 60 -> Color(0xFFFFA726)
                        else -> Color(0xFFF44336)
                    }
                )

                Text(
                    text = StringResources.accuracy(language),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(StringResources.correct(language), correctAnswers.toString(), Color(0xFF4CAF50))
                    StatColumn(StringResources.wrong(language), wrongAnswers.toString(), Color(0xFFF44336))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        when (language) {
                            Language.ENGLISH -> "Time"
                            Language.PORTUGUESE -> "Tempo"
                        },
                        "${minutes}m ${seconds}s",
                        MaterialTheme.colorScheme.primary
                    )
                    StatColumn(
                        when (language) {
                            Language.ENGLISH -> "Per Min"
                            Language.PORTUGUESE -> "Por Min"
                        },
                        String.format("%.1f", cardsPerMinute),
                        MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Back to Home Button
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = StringResources.backToHome(language),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
