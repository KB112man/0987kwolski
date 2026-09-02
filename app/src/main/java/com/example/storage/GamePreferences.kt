package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GameState
import com.example.model.MatchHistoryEntry
import com.example.model.SortMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("contract_rummy_prefs", Context.MODE_PRIVATE)

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val gameStateAdapter by lazy {
        moshi.adapter(GameState::class.java)
    }

    private val matchHistoryListAdapter by lazy {
        moshi.adapter<List<MatchHistoryEntry>>(
            Types.newParameterizedType(List::class.java, MatchHistoryEntry::class.java)
        )
    }

    fun saveGame(state: GameState) {
        try {
            val json = gameStateAdapter.toJson(state)
            prefs.edit()
                .putString("saved_game_state", json)
                .putBoolean("has_saved_game", true)
                .apply()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun saveGameState(state: GameState) = saveGame(state)

    fun loadGame(): GameState? {
        if (!hasSavedGame()) return null
        return try {
            val json = prefs.getString("saved_game_state", null) ?: return null
            gameStateAdapter.fromJson(json)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun loadGameState(): GameState? = loadGame()

    fun clearSavedGame() {
        prefs.edit()
            .remove("saved_game_state")
            .putBoolean("has_saved_game", false)
            .apply()
    }

    fun clearSavedGameState() = clearSavedGame()

    fun hasSavedGame(): Boolean {
        return prefs.getBoolean("has_saved_game", false)
    }

    fun saveMatchHistoryEntry(entry: MatchHistoryEntry) {
        try {
            val currentList = loadMatchHistory().toMutableList()
            currentList.add(0, entry)
            val json = matchHistoryListAdapter.toJson(currentList)
            prefs.edit()
                .putString("match_history_list", json)
                .apply()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun loadMatchHistory(): List<MatchHistoryEntry> {
        return try {
            val json = prefs.getString("match_history_list", null) ?: return emptyList()
            matchHistoryListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearMatchHistory() {
        prefs.edit().remove("match_history_list").apply()
    }

    var defaultSortMode: SortMode
        get() = if (prefs.getString("setting_sort", "RANK") == "SUIT") SortMode.SUIT else SortMode.RANK
        set(value) = prefs.edit().putString("setting_sort", value.name).apply()

    var aiSpeedMs: Long
        get() = prefs.getLong("setting_ai_speed", 800L)
        set(value) = prefs.edit().putLong("setting_ai_speed", value).apply()

    var playerName: String
        get() = prefs.getString("player_name", "You") ?: "You"
        set(value) = prefs.edit().putString("player_name", value).apply()
}
