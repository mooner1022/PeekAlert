package dev.mooner.peekalert.demo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dev.mooner.peekalert.PeekAlert
import dev.mooner.peekalert.createPeekAlert
import kotlin.concurrent.thread

/**
 * Cases the library has to get right, one button each, for trying them by hand or from adb:
 *
 *     adb shell am start -n dev.mooner.peekalert.demo/.BenchActivity --ez open_top true --ez edge true
 *
 * - `open_top`, `open_bottom`: show an alert while the activity is being created, before its first layout.
 * - `edge`: draw edge to edge (true by default); false leaves the system to fit the content between the bars.
 */
class BenchActivity : AppCompatActivity() {

    private var shown = 0
    private var dismissed = 0
    private lateinit var status: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val edgeToEdge = intent.getBooleanExtra("edge", true)
        WindowCompat.setDecorFitsSystemWindows(window, !edgeToEdge)

        status = TextView(this).apply { textSize = 16f }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 48, 48, 48)
            addView(TextView(context).apply {
                text = if (edgeToEdge) "Edge to edge" else "Fitted between the system bars"
                textSize = 20f
            })
            addView(status)
            addView(button("Top x1") { alert(PeekAlert.Position.Top).peek() })
            addView(button("Bottom x1") { alert(PeekAlert.Position.Bottom).peek() })
            addView(button("Top x5") { repeat(5) { alert(PeekAlert.Position.Top).peek() } })
            addView(button("Bottom x5") { repeat(5) { alert(PeekAlert.Position.Bottom).peek() } })
            addView(button("Hide during show") {
                val alert = alert(PeekAlert.Position.Top)
                alert.peek()
                window.decorView.postDelayed(alert::hide, 60)
            })
            addView(button("Hide at once") {
                val alert = alert(PeekAlert.Position.Top)
                alert.peek()
                alert.hide()
            })
            addView(button("Peek twice") {
                val alert = alert(PeekAlert.Position.Bottom)
                alert.peek()
                window.decorView.postDelayed(alert::peek, 500)
            })
            addView(button("From a thread") {
                val alert = alert(PeekAlert.Position.Top)
                thread { alert.peek() }
            })
            addView(button("Sticky with action") {
                createPeekAlert(this@BenchActivity) {
                    position = PeekAlert.Position.Bottom
                    draggable = true
                    title("Sticky")
                    text("No auto hide. Swipe it down, or use the action.")
                    action("Hide") { setOnActionListener { hide() } }
                    onDismiss { count(dismiss = true) }
                }.also { count(dismiss = false) }.peek()
            })
            addView(button("Hide on touch") {
                alert(PeekAlert.Position.Top).setAutoHide(false).setHideOnTouch(true).peek()
            })
            addView(button("Long text") {
                alert(PeekAlert.Position.Bottom)
                    .setText("A longer message that needs more than two lines to be read in full, as error messages " +
                        "often do: this one goes on until it has filled four lines of the alert and then some more.")
                    .peek()
            })
            addView(button("Outlined, raised") {
                count(dismiss = false)
                createPeekAlert(this@BenchActivity) {
                    position = PeekAlert.Position.Top
                    draggable = true
                    autoHideMillis = 4000L
                    cornerRadius = 14 * resources.displayMetrics.density
                    paddingDp = 12
                    iconSizeDp = 20
                    elevationDp = 6f
                    backgroundColor(value = Color.WHITE)
                    stroke(value = Color.parseColor("#DEDEE5"))
                    iconTint(value = Color.parseColor("#D6335C"))
                    title("Outlined and raised $shown") { textColor(value = Color.parseColor("#3A3A3F")) }
                    text("A line and a shadow around the card, and an action in the text's color.") {
                        textColor(value = Color.parseColor("#707076"))
                    }
                    action("Details") { setOnActionListener { hide() } }
                    onDismiss { count(dismiss = true) }
                }.peek()
            })
            addView(button("In a dialog") {
                val dialog = AlertDialog.Builder(this@BenchActivity)
                    .setTitle("Dialog")
                    .setMessage("An alert is shown over this dialog.")
                    .setPositiveButton("Close", null)
                    .show()
                PeekAlert.create(dialog).setPosition(PeekAlert.Position.Top).setText("Over the dialog").setAutoHide(true).peek()
            })
            addView(button("Finish while showing") {
                alert(PeekAlert.Position.Top).peek()
                alert(PeekAlert.Position.Bottom).peek()
                window.decorView.postDelayed(::finish, 400)
            })
            addView(EditText(context).apply {
                hint = "Tap to open the keyboard"
                tag = "input"
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#EDEDF1"))
            addView(column, MATCH_PARENT, WRAP_CONTENT)
        }
        setContentView(scroll)
        if (edgeToEdge)
            ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                view.updatePadding(top = bars.top, bottom = bars.bottom)
                // As apps often do, and what keeps later siblings from seeing the insets before Android 11.
                WindowInsetsCompat.CONSUMED
            }
        count(null)

        if (intent.getBooleanExtra("open_top", false))
            alert(PeekAlert.Position.Top).setText("Shown while the activity was created").peek()
        if (intent.getBooleanExtra("open_bottom", false))
            alert(PeekAlert.Position.Bottom).setText("Shown while the activity was created").peek()
    }

    private fun alert(position: PeekAlert.Position): PeekAlert {
        count(dismiss = false)
        return PeekAlert.create(this)
            .setPosition(position)
            .setDraggable(true)
            .setText("Alert $shown")
            .setAutoHide(2500L)
            .setOnDismissListener { count(dismiss = true) }
    }

    @SuppressLint("SetTextI18n")
    private fun count(dismiss: Boolean?) {
        when (dismiss) {
            true  -> dismissed++
            false -> shown++
            null  -> {}
        }
        status.text = "shown $shown · dismissed $dismissed"
    }

    private fun button(label: String, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        }
}
