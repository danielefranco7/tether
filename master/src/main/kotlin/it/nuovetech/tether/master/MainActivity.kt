package it.nuovetech.tether.master

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Schermata della Master: toggle di abilitazione, stato dell'accesso alle notifiche e elenco in coda (REQ-PAU-01/02). */
class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("tether_master", Context.MODE_PRIVATE) }
    private val listenerComponent by lazy { ComponentName(this, NotificationForwarderService::class.java) }
    private val outbox by lazy { NotificationOutbox.get(this) }
    private val listAdapter by lazy { NotificationListAdapter(this) }
    private val emptyText by lazy { findViewById<TextView>(R.id.emptyText) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val list = findViewById<ListView>(R.id.notificationList)
        list.adapter = listAdapter
        list.setOnItemLongClickListener { _, _, position, _ ->
            confirmDelete(listAdapter.getItem(position))
            true
        }

        val root = findViewById<View>(R.id.rootLayout)
        val basePadding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                basePadding + bars.left,
                basePadding + bars.top,
                basePadding + bars.right,
                basePadding + bars.bottom,
            )
            insets
        }

        val toggle = findViewById<Switch>(R.id.masterToggle)
        val status = findViewById<TextView>(R.id.statusText)
        val grant = findViewById<Button>(R.id.grantAccessButton)

        toggle.isChecked = prefs.getBoolean(KEY_ENABLED, false)
        applyState(toggle.isChecked, status)

        toggle.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_ENABLED, checked).apply()
            setListenerEnabled(checked)
            applyState(checked, status)
        }

        grant.setOnClickListener {
            startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_APP_RULES, 0, R.string.menu_app_rules)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_APP_RULES) {
            startActivity(Intent(this, AppRulesActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        updateAccessState()
    }

    override fun onStart() {
        super.onStart()
        refreshList()
        findViewById<TextView>(R.id.relayText).text = getString(R.string.relay_url, BuildConfig.RELAY_URL)
        showRelayState(NotificationForwarderService.relayState)
        NotificationForwarderService.onRelayStateChanged = { state -> runOnUiThread { showRelayState(state) } }
        outbox.onChanged = { runOnUiThread { refreshList() } }
    }

    override fun onStop() {
        NotificationForwarderService.onRelayStateChanged = null
        outbox.onChanged = null
        super.onStop()
    }

    private fun showRelayState(state: RelayClient.State) {
        val text = if (state == RelayClient.State.CONNECTED) R.string.relay_connected else R.string.relay_disconnected
        findViewById<TextView>(R.id.relayText).setText(text)
    }

    /** Aggiorna l'elenco dalla [NotificationOutbox]: più recenti in alto, stato sempre aggiornato. */
    private fun refreshList() {
        val entries = outbox.snapshot()
        listAdapter.submit(entries)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Pressione lunga su una riga: conferma e cancellazione dalla coda. */
    private fun confirmDelete(entry: OutboxEntry) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(R.string.delete) { _, _ -> outbox.remove(entry.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Verifica se l'accesso alle notifiche è stato concesso. Se manca, avvisa l'utente e mostra
     * il pulsante per concederlo; altrimenti mostra la conferma.
     */
    private fun updateAccessState() {
        val text = findViewById<TextView>(R.id.accessText)
        val grant = findViewById<Button>(R.id.grantAccessButton)
        val granted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

        if (granted) {
            text.setText(R.string.access_granted)
            grant.visibility = View.GONE
        } else {
            text.setText(R.string.access_missing)
            grant.visibility = View.VISIBLE
        }
    }

    /**
     * Abilita o disabilita il componente di ascolto. Con il componente disabilitato il sistema
     * non collega il servizio, quindi la Master è inattiva (REQ-PRF-04).
     */
    private fun setListenerEnabled(enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(listenerComponent, state, PackageManager.DONT_KILL_APP)
    }

    private fun applyState(enabled: Boolean, status: TextView) {
        status.text = if (enabled) getString(R.string.status_enabled) else getString(R.string.status_disabled)
    }

    companion object {
        private const val KEY_ENABLED = "master_enabled"
        private const val MENU_APP_RULES = 1
    }
}
