package okano.dev.android.derdiedas.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okano.dev.android.derdiedas.data.database.GameSessionEntity
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.ui.resources.StringResources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Screen displaying history of past game sessions
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    language: Language,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameSessions by viewModel.gameSessions.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = StringResources.gameHistory(language),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (gameSessions.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = StringResources.noGamesYet(language),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "Start a new game to see your history"
                        Language.PORTUGUESE -> "Inicie um novo jogo para ver seu histórico"
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            // Game sessions list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(gameSessions) { session ->
                    GameSessionCard(
                        session = session,
                        language = language,
                        onDeleteClick = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear All History Button (only show if there are sessions)
        if (gameSessions.isNotEmpty()) {
            OutlinedButton(
                onClick = { showClearAllDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF44336)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = StringResources.delete(language),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = StringResources.clearAllHistory(language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Back Button
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = when (language) {
                    Language.ENGLISH -> "Back to Main Menu"
                    Language.PORTUGUESE -> "Voltar ao Menu Principal"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Confirmation dialog for clearing all history
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    when (language) {
                        Language.ENGLISH -> "Clear All History?"
                        Language.PORTUGUESE -> "Limpar Todo Histórico?"
                    }
                )
            },
            text = {
                Text(StringResources.confirmClearHistory(language))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text(
                        when (language) {
                            Language.ENGLISH -> "Delete All"
                            Language.PORTUGUESE -> "Excluir Tudo"
                        },
                        color = Color(0xFFF44336)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(StringResources.cancel(language))
                }
            }
        )
    }
}

@Composable
private fun GameSessionCard(
    session: GameSessionEntity,
    language: Language,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(session.timestamp))

    val minutes = TimeUnit.MILLISECONDS.toMinutes(session.durationMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(session.durationMillis) % 60

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete session",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accuracy
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "${session.accuracyPercentage}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            session.accuracyPercentage >= 80 -> Color(0xFF4CAF50)
                            session.accuracyPercentage >= 60 -> Color(0xFFFFA726)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = StringResources.accuracy(language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Cards
                StatItem(
                    "${session.totalCards} ${StringResources.cards(language)}",
                    when (language) {
                        Language.ENGLISH -> "Total"
                        Language.PORTUGUESE -> "Total"
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Additional stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = session.correctAnswers.toString(),
                    label = StringResources.correct(language),
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    value = session.wrongAnswers.toString(),
                    label = StringResources.wrong(language),
                    color = Color(0xFFF44336)
                )
                StatItem(
                    value = "${minutes}m ${seconds}s",
                    label = when (language) {
                        Language.ENGLISH -> "Time"
                        Language.PORTUGUESE -> "Tempo"
                    }
                )
                StatItem(
                    value = String.format("%.1f", session.cardsPerMinute),
                    label = when (language) {
                        Language.ENGLISH -> "Per Min"
                        Language.PORTUGUESE -> "Por Min"
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
