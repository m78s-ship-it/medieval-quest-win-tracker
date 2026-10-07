package com.example.medievalquestwintracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quest_data")

@Serializable
data class PlayerStats(
    val level: Int = 1,
    val xp: Int = 0,
    val totalXpEarned: Int = 0,
    val streak: Int = 0,
    val lastQuestDate: String = "",
    val title: String = "Novice Adventurer"
) {
    fun getTitleByLevel(): String = when {
        level >= 50 -> "🐉 Legendary Hero"
        level >= 40 -> "👑 Grand Champion"
        level >= 30 -> "🛡️ Noble Knight"
        level >= 20 -> "⚔️ Seasoned Warrior"
        level >= 10 -> "🗡️ Battle-Hardened"
        level >= 5 -> "📚 Skilled Squire"
        else -> "🌟 Novice Adventurer"
    }

    fun getXpForNextLevel(): Int = level * 100

    fun getLevelProgress(): Float = xp.toFloat() / getXpForNextLevel().toFloat()
}

@Serializable
data class QuestData(
    val id: Int = 0,
    val title: String = "",
    val xp: Int = 15,
    val completed: Boolean = false,
    val completedDate: String = "",
    val isDaily: Boolean = false
)

class StorageManager(private val context: Context) {
    private val JSON = Json { ignoreUnknownKeys = true }
    private val PLAYER_STATS_KEY = stringPreferencesKey("player_stats")
    private val QUESTS_KEY = stringPreferencesKey("quests")

    fun getPlayerStatsFlow(): Flow<PlayerStats> = context.dataStore.data.map { preferences ->
        val statsJson = preferences[PLAYER_STATS_KEY] ?: return@map PlayerStats()
        try {
            JSON.decodeFromString<PlayerStats>(statsJson)
        } catch (e: Exception) {
            PlayerStats()
        }
    }

    fun getQuestsFlow(): Flow<List<QuestData>> = context.dataStore.data.map { preferences ->
        val questsJson = preferences[QUESTS_KEY] ?: return@map emptyList()
        try {
            JSON.decodeFromString<List<QuestData>>(questsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePlayerStats(stats: PlayerStats) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_STATS_KEY] = JSON.encodeToString(PlayerStats.serializer(), stats)
        }
    }

    suspend fun saveQuests(quests: List<QuestData>) {
        context.dataStore.edit { preferences ->
            preferences[QUESTS_KEY] = JSON.encodeToString(quests)
        }
    }
}

fun getCurrentDateString(): String {
    return LocalDate.now().format(DateTimeFormatter.ISO_DATE)
}

fun isSameDayAsToday(dateString: String): Boolean {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        date == LocalDate.now()
    } catch (e: Exception) {
        false
    }
}
