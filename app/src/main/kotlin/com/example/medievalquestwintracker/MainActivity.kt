package com.example.medievalquestwintracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Crown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedievalQuestWinTrackerTheme {
                MedievalQuestWinTrackerApp()
            }
        }
    }
}

@Composable
fun MedievalQuestWinTrackerTheme(content: @Composable () -> Unit) {
    val darkFantasyColors = darkColorScheme(
        primary = Color(0xFFD4AF37),
        secondary = Color(0xFF8B1E3F),
        tertiary = Color(0xFF1F3B4D),
        background = Color(0xFF100C14),
        surface = Color(0xFF1A1820),
        onPrimary = Color(0xFF1A1200),
        onBackground = Color(0xFFF4EADC),
        onSurface = Color(0xFFF4EADC)
    )

    MaterialTheme(
        colorScheme = darkFantasyColors,
        content = content
    )
}

@Composable
fun MedievalQuestWinTrackerApp() {
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }
    val scope = rememberCoroutineScope()

    val playerStats by storageManager.getPlayerStatsFlow().collectAsState(initial = PlayerStats())
    val quests by storageManager.getQuestsFlow().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var customQuestTitle by remember { mutableStateOf(TextFieldValue()) }
    var lastLevelUpXp by remember { mutableStateOf(0) }

    val completedQuests = quests.count { it.completed }
    val currentXp = playerStats.xp
    val level = playerStats.level
    val currentXpForLevel = playerStats.getXpForNextLevel()
    val levelProgress = playerStats.getLevelProgress()

    val completedTodayCount = quests.count { it.completed && isSameDayAsToday(it.completedDate) }
    val hasDailyQuestToday = quests.any { it.isDaily && isSameDayAsToday(it.completedDate) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    SoundManager.playButtonClickSound(context)
                },
                containerColor = Color(0xFFD4AF37),
                contentColor = Color(0xFF1A1200)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add quest")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color(0xFF100C14)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF100C14),
                            Color(0xFF1A1820),
                            Color(0xFF261C24)
                        )
                    )
                )
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WarriorCard(playerStats)

            LevelProgressCard(level, currentXp, currentXpForLevel, levelProgress)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Wins", "$completedQuests", Color(0xFFD4AF37))
                StatCard("Streak", "${playerStats.streak}", Color(0xFF8B1E3F))
                StatCard("Today", "$completedTodayCount", Color(0xFF2C7A7B))
            }

            if (!hasDailyQuestToday) {
                DailyQuestPrompt(
                    onAddDaily = {
                        scope.launch {
                            val newDaily = QuestData(
                                id = (quests.maxOfOrNull { it.id } ?: 0) + 1,
                                title = "Daily Challenge",
                                xp = 50,
                                isDaily = true
                            )
                            val updatedQuests = quests + newDaily
                            storageManager.saveQuests(updatedQuests)
                        }
                    }
                )
            }

            Button(
                onClick = {
                    SoundManager.playButtonClickSound(context)
                    val incompleteQuest = quests.firstOrNull { !it.completed }
                    if (incompleteQuest != null) {
                        scope.launch {
                            val updatedQuest = incompleteQuest.copy(
                                completed = true,
                                completedDate = getCurrentDateString()
                            )
                            val updatedQuests = quests.map {
                                if (it.id == incompleteQuest.id) updatedQuest else it
                            }
                            storageManager.saveQuests(updatedQuests)

                            // Update player stats
                            val newXp = currentXp + incompleteQuest.xp
                            val wasLeveledUp = (newXp / currentXpForLevel) > (currentXp / currentXpForLevel)
                            val newLevel = if (wasLeveledUp) level + 1 else level
                            val xpAfterLevel = if (wasLeveledUp) newXp - currentXpForLevel else newXp

                            val newStreak = if (isSameDayAsToday(playerStats.lastQuestDate)) {
                                playerStats.streak
                            } else {
                                playerStats.streak + 1
                            }

                            val newStats = playerStats.copy(
                                xp = xpAfterLevel,
                                level = newLevel,
                                totalXpEarned = playerStats.totalXpEarned + incompleteQuest.xp,
                                streak = newStreak,
                                lastQuestDate = getCurrentDateString(),
                                title = playerStats.getTitleByLevel()
                            )
                            storageManager.savePlayerStats(newStats)

                            if (wasLeveledUp) {
                                SoundManager.playLevelUpSound(context)
                                showLevelUpDialog = true
                            } else {
                                SoundManager.playQuestCompleteSound(context)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD4AF37),
                    contentColor = Color(0xFF1A1200)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("⚔️ Complete Next Quest", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Text(
                text = "Quest Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF4EADC)
            )

            if (quests.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1820))
                ) {
                    Text(
                        text = "No quests yet. Your trail is empty.",
                        modifier = Modifier.padding(20.dp),
                        color = Color(0xFFD8C89A)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quests) { quest ->
                        QuestCard(
                            quest = quest,
                            onToggle = {
                                scope.launch {
                                    val updatedQuest = quest.copy(
                                        completed = !quest.completed,
                                        completedDate = if (!quest.completed) getCurrentDateString() else ""
                                    )
                                    val updatedQuests = quests.map {
                                        if (it.id == quest.id) updatedQuest else it
                                    }
                                    storageManager.saveQuests(updatedQuests)

                                    if (!quest.completed) {
                                        val newXp = currentXp + quest.xp
                                        val xpForLevel = playerStats.getXpForNextLevel()
                                        val wasLeveledUp = newXp >= xpForLevel
                                        val newLevel = if (wasLeveledUp) level + 1 else level
                                        val xpAfterLevel = if (wasLeveledUp) newXp - xpForLevel else newXp

                                        val newStreak = if (isSameDayAsToday(playerStats.lastQuestDate)) {
                                            playerStats.streak
                                        } else {
                                            playerStats.streak + 1
                                        }

                                        val newStats = playerStats.copy(
                                            xp = xpAfterLevel,
                                            level = newLevel,
                                            totalXpEarned = playerStats.totalXpEarned + quest.xp,
                                            streak = newStreak,
                                            lastQuestDate = getCurrentDateString(),
                                            title = playerStats.getTitleByLevel()
                                        )
                                        storageManager.savePlayerStats(newStats)

                                        if (wasLeveledUp) {
                                            SoundManager.playLevelUpSound(context)
                                            showLevelUpDialog = true
                                        } else {
                                            SoundManager.playQuestCompleteSound(context)
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    val updatedQuests = quests.filter { it.id != quest.id }
                                    storageManager.saveQuests(updatedQuests)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddQuestDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { questTitle ->
                scope.launch {
                    val newQuest = QuestData(
                        id = (quests.maxOfOrNull { it.id } ?: 0) + 1,
                        title = questTitle,
                        xp = 15,
                        isDaily = false
                    )
                    val updatedQuests = quests + newQuest
                    storageManager.saveQuests(updatedQuests)
                }
                customQuestTitle = TextFieldValue()
                showAddDialog = false
            }
        )
    }

    if (showLevelUpDialog) {
        LevelUpDialog(
            newLevel = level,
            onDismiss = { showLevelUpDialog = false }
        )
    }
}

@Composable
fun WarriorCard(playerStats: PlayerStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF251C2D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFD4AF37), Color(0xFFF0E68C))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Crown,
                    contentDescription = null,
                    tint = Color(0xFF1A1200),
                    modifier = Modifier.size(56.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Level ${playerStats.level}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD4AF37)
                )
                Text(
                    text = playerStats.getTitleByLevel(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF4EADC)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "${playerStats.totalXpEarned} Total XP",
                        color = Color(0xFFD8C89A),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun LevelProgressCard(level: Int, currentXp: Int, xpForLevel: Int, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A26))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "XP Progress",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4EADC)
                )
                Text(
                    text = "$currentXp / $xpForLevel",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4AF37),
                    fontSize = 12.sp
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = Color(0xFFD4AF37),
                trackColor = Color(0xFF3A2E1F),
                drawStopIndicator = {}
            )

            Text(
                text = "${(progress * 100).roundToInt().coerceIn(0, 100)}% to next level",
                color = Color(0xFFD8C89A),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, accent: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1A22)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFFD8C89A),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = value,
                color = accent,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DailyQuestPrompt(onAddDaily: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2E1F)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFF0A020),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No Daily Quest Today!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4EADC)
                )
                Text(
                    text = "Complete one for extra XP",
                    color = Color(0xFFD8C89A),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onAddDaily,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Add", color = Color(0xFF1A1200), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuestCard(
    quest: QuestData,
    onToggle: (QuestData) -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (quest.completed) 0.95f else 1f,
        animationSpec = spring()
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (quest.completed) Color(0xFF2A2432) else Color(0xFF1E1A26")
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = quest.completed,
                onCheckedChange = { onToggle(quest) },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFD4AF37),
                    uncheckedColor = Color(0xFFD8C89A)
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    color = Color(0xFFF4EADC),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (quest.isDaily) Icons.Default.LocalFireDepartment else Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (quest.isDaily) Color(0xFFF0A020) else Color(0xFFD4AF37),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "${quest.xp} XP",
                        color = Color(0xFFD8C89A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (quest.completed) Color(0xFF1F3B4D) else Color(0xFF3A2E1F),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        onDelete()
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (quest.completed) "✓" else "✕",
                    color = if (quest.completed) Color(0xFFBDE2EA) else Color(0xFFF0D7A5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AddQuestDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var questTitle by remember { mutableStateOf(TextFieldValue()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A26))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Quest",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4EADC)
                )

                OutlinedTextField(
                    value = questTitle,
                    onValueChange = { questTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quest name") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val trimmed = questTitle.text.trim()
                            if (trimmed.isNotEmpty()) {
                                onAdd(trimmed)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text("Create", color = Color(0xFF1A1200))
                    }
                }
            }
        }
    }
}

@Composable
fun LevelUpDialog(newLevel: Int, onDismiss: () -> Unit) {
    var showAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showAnimation = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF251C2D))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = showAnimation,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(80.dp)
                    )
                }

                Text(
                    text = "LEVEL UP!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD4AF37)
                )

                Text(
                    text = "You reached Level $newLevel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF4EADC)
                )

                Text(
                    text = "You are becoming a legend!",
                    color = Color(0xFFD8C89A),
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Continue", color = Color(0xFF1A1200), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedievalQuestWinTrackerPreview() {
    MedievalQuestWinTrackerTheme {
        // Preview placeholder
    }
}
