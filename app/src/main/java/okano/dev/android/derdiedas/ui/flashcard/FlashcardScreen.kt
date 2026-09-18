package okano.dev.android.derdiedas.ui.flashcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import okano.dev.android.derdiedas.data.model.Article
import okano.dev.android.derdiedas.ui.resources.StringResources

@Composable
fun FlashcardScreen(
    viewModel: FlashcardViewModel,
    onGameEnd: (correct: Int, wrong: Int, duration: Long, cardsPerMinute: Float) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Navigate to results once the finished game has been saved
    val gameResult = uiState.gameResult
    LaunchedEffect(gameResult) {
        gameResult?.let {
            onGameEnd(it.correctAnswers, it.wrongAnswers, it.durationMillis, it.cardsPerMinute)
        }
    }

    // Handle lifecycle events for pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseTimer()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeTimer()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button at the top
        IconButton(
            onClick = {
                viewModel.cancelGame()
                onBackClick()
            }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = StringResources.back(uiState.language),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Statistics at the top
        StatisticsBar(
            correctAnswers = uiState.correctAnswers,
            wrongAnswers = uiState.wrongAnswers,
            accuracyPercentage = uiState.accuracyPercentage,
            currentCard = uiState.currentCardIndex,
            totalCards = uiState.totalCards,
            elapsedTimeMillis = uiState.elapsedTimeMillis,
            language = uiState.language
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Animated Flashcard only
                AnimatedContent(
                    targetState = uiState.currentNoun,
                    transitionSpec = {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 400)
                        ) + fadeIn(animationSpec = tween(durationMillis = 400)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(durationMillis = 400)
                                ) + fadeOut(animationSpec = tween(durationMillis = 400))
                            )
                    },
                    label = "card_animation"
                ) { currentNoun ->
                    NounCard(
                        noun = currentNoun?.noun ?: "",
                        translation = currentNoun?.getTranslation(uiState.language) ?: "",
                        isAnswered = uiState.isAnswered,
                        isCorrect = uiState.isCorrect,
                        correctArticle = currentNoun?.article?.getDisplayName() ?: "",
                        language = uiState.language
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Article buttons (not animated)
                ArticleButtons(
                    onArticleSelected = { article ->
                        if (!uiState.isAnswered) {
                            viewModel.onArticleSelected(article)
                        }
                    },
                    isAnswered = uiState.isAnswered,
                    selectedArticle = uiState.selectedArticle,
                    correctArticle = uiState.currentNoun?.article
                )
            }
        }
    }
}

@Composable
fun StatisticsBar(
    correctAnswers: Int,
    wrongAnswers: Int,
    accuracyPercentage: Int,
    currentCard: Int,
    totalCards: Int,
    elapsedTimeMillis: Long,
    language: okano.dev.android.derdiedas.data.model.Language
) {
    // Format time as MM:SS
    val minutes = (elapsedTimeMillis / 1000 / 60).toInt()
    val seconds = (elapsedTimeMillis / 1000 % 60).toInt()
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Card Progress and Timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (language) {
                    okano.dev.android.derdiedas.data.model.Language.ENGLISH -> "Card $currentCard of $totalCards"
                    okano.dev.android.derdiedas.data.model.Language.PORTUGUESE -> "Carta $currentCard de $totalCards"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "⏱ $timeString",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = StringResources.correct(language), value = correctAnswers.toString(), color = Color(0xFF4CAF50))
                StatItem(label = StringResources.wrong(language), value = wrongAnswers.toString(), color = Color(0xFFF44336))
                StatItem(label = StringResources.accuracy(language), value = "$accuracyPercentage%", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NounCard(
    noun: String,
    translation: String,
    isAnswered: Boolean,
    isCorrect: Boolean,
    correctArticle: String,
    language: okano.dev.android.derdiedas.data.model.Language
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isAnswered -> MaterialTheme.colorScheme.surface
                isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                else -> Color(0xFFF44336).copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isAnswered) {
                Text(
                    text = if (isCorrect) StringResources.correctFeedback(language) else StringResources.wrongFeedback(language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = noun,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = translation,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (isAnswered) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "$correctArticle $noun",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ArticleButtons(
    onArticleSelected: (Article) -> Unit,
    isAnswered: Boolean,
    selectedArticle: Article?,
    correctArticle: Article?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Article.entries.forEach { article ->
            val isSelected = selectedArticle == article
            val isCorrectAnswer = correctArticle == article

            val buttonColor = when {
                !isAnswered -> MaterialTheme.colorScheme.primary
                isSelected && isCorrectAnswer -> Color(0xFF4CAF50)
                isSelected && !isCorrectAnswer -> Color(0xFFF44336)
                !isSelected && isCorrectAnswer && isAnswered -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            }

            Button(
                onClick = { onArticleSelected(article) },
                enabled = !isAnswered,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = buttonColor
                )
            ) {
                Text(
                    text = article.getDisplayName(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}