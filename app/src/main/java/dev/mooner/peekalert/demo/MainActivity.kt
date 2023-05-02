package dev.mooner.peekalert.demo

import android.content.res.Resources
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams
import androidx.core.content.res.ResourcesCompat
import dev.mooner.peekalert.PeekAlert
import dev.mooner.peekalert.createPeekAlert
import dev.mooner.peekalert.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonTop.setOnClickListener {
            val peekAlert = PeekAlert
                .create(this)
                .setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                .setPosition(PeekAlert.Position.Top)
                //.setHideOnTouch(true)
                .setDraggable(true)
            peekAlert.peek()
        }

        binding.buttonBottom.setOnClickListener {
            val context = this
            createPeekAlert(this) {
                position = PeekAlert.Position.Bottom
                cornerRadius = 12.dp.toFloat()
                paddingDp = 14
                width = LayoutParams.MATCH_PARENT
                iconRes = dev.mooner.peekalert.R.drawable.round_info_24
                draggable = true
                hideOnTouch = true
                iconTint(android.R.color.white)
                backgroundColor(value = Color.parseColor("#E06469"))
                title("JobLocker") {
                    textColor(res = android.R.color.white)
                    textSize = 14f
                    typeface = ResourcesCompat.getFont(context, R.font.nanumsquare_round_bold)
                }
                text("Failed to release job `Project-Discord-test-worker-3`") {
                    textColor(res = android.R.color.white)
                    textSize = 12f
                    typeface = ResourcesCompat.getFont(context, R.font.nanumsquare_round_regular)
                }
                action("+") {
                    backgroundColor(value = Color.parseColor("#FFFFFF"))
                    textColor(value = getColor(com.google.android.material.R.color.m3_default_color_primary_text))
                    setOnActionListener { _ ->
                        hide()
                    }
                }
                autoHideMillis = 4000L
            }.peek()
        }
    }

    private val Number.dp get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}