package it.nuovetech.tether.slave

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Schermata della Slave. Lo scheletro mostra solo lo stato; la lista eventi arriverà con il canale. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.statusText).setText(R.string.status_waiting)
    }
}
