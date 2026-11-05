package it.nuovetech.tether.master

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.Date

/** Elenco delle notifiche in coda: icona app a sinistra, titolo, testo, ora e stato (in coda, trasmesso, ACK). */
class NotificationListAdapter(private val context: Context) : BaseAdapter() {

    private var items: List<OutboxEntry> = emptyList()

    fun submit(entries: List<OutboxEntry>) {
        items = entries
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): OutboxEntry = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false)
        val entry = getItem(position)

        view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(iconFor(entry.app))
        view.findViewById<TextView>(R.id.appLabel).text = entry.appLabel
        view.findViewById<TextView>(R.id.postedTime).text =
            DateFormat.getTimeFormat(context).format(Date(entry.postedAt))
        view.findViewById<TextView>(R.id.titleText).text = entry.title
        view.findViewById<TextView>(R.id.bodyText).text = entry.text

        val status = view.findViewById<TextView>(R.id.statusText)
        val (symbol, label, color) = when (entry.status) {
            OutboxEntry.Status.QUEUED -> Triple("○", R.string.status_queued, 0xFF8E8E93.toInt())
            OutboxEntry.Status.SENT -> Triple("✓", R.string.status_sent, 0xFF8E8E93.toInt())
            OutboxEntry.Status.ACKED -> Triple("✓✓", R.string.status_acked, 0xFF2E7D32.toInt())
        }
        status.text = "$symbol ${context.getString(label)}"
        status.setTextColor(color)
        return view
    }

    private fun iconFor(packageName: String): Drawable =
        runCatching { context.packageManager.getApplicationIcon(packageName) }
            .getOrElse { context.packageManager.defaultActivityIcon }
}
