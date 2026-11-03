package it.nuovetech.tether.master

import android.content.Context
import it.nuovetech.tether.shared.NotificationLevel

/**
 * Regole di inoltro per app (SPECIFICATION.md, sezione 3.1). Ogni package ha un livello
 * scelto dall'utente. Le app senza regola sono scartate (livello 0), come da default.
 */
class LevelRules(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun levelFor(packageName: String): NotificationLevel {
        val code = prefs.getInt(packageName, NotificationLevel.DISCARDED.code)
        return NotificationLevel.entries.firstOrNull { it.code == code } ?: NotificationLevel.DISCARDED
    }

    fun setLevel(packageName: String, level: NotificationLevel) {
        prefs.edit().putInt(packageName, level.code).apply()
    }

    private companion object {
        const val PREFS = "tether_level_rules"
    }
}
