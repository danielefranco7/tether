package it.nuovetech.tether.master

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

/** Schermata della Master: toggle di abilitazione e stato dell'accesso alle notifiche (REQ-PAU-01/02). */
class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("tether_master", Context.MODE_PRIVATE) }
    private val listenerComponent by lazy { ComponentName(this, NotificationForwarderService::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    override fun onResume() {
        super.onResume()
        updateAccessState()
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
    }
}
