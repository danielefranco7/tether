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

    /** Registra un package che ha inviato una notifica, così compare nell'elenco anche senza icona nel launcher. */
    fun rememberPackage(packageName: String) {
        val seen = prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()
        if (packageName !in seen) {
            prefs.edit().putStringSet(KEY_SEEN, seen + packageName).apply()
        }
    }

    fun seenPackages(): Set<String> = prefs.getStringSet(KEY_SEEN, emptySet()) ?: emptySet()

    private companion object {
        const val PREFS = "tether_level_rules"
        const val KEY_SEEN = "seen_packages"
    }
}
