package it.nuovetech.tether.slave

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.Date
import kotlin.math.abs

/** Elenco degli eventi ricevuti: icona app a sinistra, titolo, testo, ora e stato di ricezione. */
class EventListAdapter(private val context: Context) : BaseAdapter() {

    private var items: List<ReceivedEvent> = emptyList()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    /** Chiamato quando l'utente fa swipe (in entrambe le direzioni) su una riga: rimuove la voce. */
    var onSwipeDelete: ((ReceivedEvent) -> Unit)? = null

    fun submit(events: List<ReceivedEvent>) {
        items = events
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): ReceivedEvent = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_event, parent, false)
        val event = getItem(position)

        view.animate().cancel()
        view.translationX = 0f
        view.alpha = 1f
        view.setOnTouchListener(swipeListener(view, event))

        view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(iconFor(event.app))
        view.findViewById<TextView>(R.id.appLabel).text = event.appLabel
        view.findViewById<TextView>(R.id.postedTime).text =
            DateFormat.getTimeFormat(context).format(Date(event.postedAt))
        view.findViewById<TextView>(R.id.titleText).text = event.title
        view.findViewById<TextView>(R.id.bodyText).text = event.text

        val status = view.findViewById<TextView>(R.id.statusText)
        status.text = "✓✓ ${context.getString(R.string.status_received)}"
        status.setTextColor(0xFF2E7D32.toInt())
        return view
    }

    /**
     * Swipe orizzontale: la riga segue il dito; oltre il 40% della larghezza scorre via e viene rimossa.
     * Il movimento verticale resta allo scroll della lista.
     */
    private fun swipeListener(row: View, event: ReceivedEvent) = object : View.OnTouchListener {
        private var downX = 0f
        private var dragging = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    dragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    if (!dragging && abs(dx) > touchSlop) {
                        dragging = true
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (!dragging) return false
                    row.translationX = dx
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) return false
                    val dx = e.rawX - downX
                    dragging = false
                    if (abs(dx) > row.width * 0.4f) {
                        val target = if (dx > 0) row.width.toFloat() else -row.width.toFloat()
                        row.animate().translationX(target).alpha(0f).setDuration(150)
                            .withEndAction { onSwipeDelete?.invoke(event) }
                            .start()
                    } else {
                        row.animate().translationX(0f).setDuration(150).start()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) row.animate().translationX(0f).setDuration(150).start()
                    dragging = false
                    return false
                }
            }
            return false
        }
    }

    private fun iconFor(packageName: String): Drawable =
        runCatching { context.packageManager.getApplicationIcon(packageName) }
            .getOrElse { context.packageManager.defaultActivityIcon }
}
