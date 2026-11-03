package it.nuovetech.tether.master

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import it.nuovetech.tether.shared.NotificationLevel

/**
 * Configurazione del livello di inoltro per ogni app installata (SPECIFICATION.md, sezione 3.1).
 * Tocco su un'app: scelta del livello 0, 1 o 2.
 */
class AppRulesActivity : AppCompatActivity() {

    private val rules by lazy { LevelRules(this) }
    private val apps = mutableListOf<AppEntry>()

    private data class AppEntry(val packageName: String, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_rules)
        title = getString(R.string.app_rules_title)

        apps += loadLaunchableApps()

        val list = findViewById<ListView>(R.id.appList)
        val adapter = AppAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            cycleLevel(apps[position], adapter)
        }
    }

    /**
     * Unisce le app con icona nel launcher e i package che hanno già inviato notifiche.
     * Da Android 11 la sola query sul launcher non mostra app come telefono o messaggi,
     * che spesso non hanno icona.
     */
    private fun loadLaunchableApps(): List<AppEntry> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val fromLauncher = packageManager.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
        val packages = (fromLauncher + rules.seenPackages()).toSet() - packageName

        return packages
            .map { AppEntry(it, labelOf(it)) }
            .sortedBy { it.label.lowercase() }
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    /** Tocco: il livello passa al successivo nel ciclo 0 → 1 → 2 → 0. */
    private fun cycleLevel(entry: AppEntry, adapter: AppAdapter) {
        val levels = NotificationLevel.entries
        val next = levels[(rules.levelFor(entry.packageName).ordinal + 1) % levels.size]
        rules.setLevel(entry.packageName, next)
        adapter.notifyDataSetChanged()
    }

    private fun levelLabel(level: NotificationLevel): String = when (level) {
        NotificationLevel.DISCARDED -> getString(R.string.level_discarded)
        NotificationLevel.SILENT -> getString(R.string.level_silent)
        NotificationLevel.ALERT -> getString(R.string.level_alert)
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): Any = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            val entry = apps[position]
            view.findViewById<TextView>(android.R.id.text1).text = entry.label
            view.findViewById<TextView>(android.R.id.text2).text =
                levelLabel(rules.levelFor(entry.packageName))
            return view
        }
    }
}
