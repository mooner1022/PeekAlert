package dev.mooner.peekalert.demo

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import dev.mooner.peekalert.PeekAlert
import dev.mooner.peekalert.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val peekAlert = PeekAlert.create(this)
            .setTitleTypeface(ResourcesCompat.getFont(this, R.font.nanumsquare_round_bold)!!)
            .setTextTypeface(ResourcesCompat.getFont(this, R.font.nanumsquare_round_regular)!!)
            .setDraggable(true)
            .setTitle("Hello, PeekAlert!")
            .setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")

        binding.togglePosition.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked)
                return@addOnButtonCheckedListener

            val position = when(checkedId) {
                R.id.buttonTop ->
                    PeekAlert.Position.Top
                R.id.buttonBottom ->
                    PeekAlert.Position.Bottom
                else -> return@addOnButtonCheckedListener
            }
            peekAlert.setPosition(position)
        }

        binding.toggleWidth.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked)
                return@addOnButtonCheckedListener

            val width = when(checkedId) {
                R.id.buttonWrapContent ->
                    LayoutParams.WRAP_CONTENT
                R.id.buttonMatchParent ->
                    LayoutParams.MATCH_PARENT
                else -> return@addOnButtonCheckedListener
            }

            peekAlert.setWidth(width)
        }

        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            peekAlert.setCornerRadius(value.dp.toFloat())
        }

        binding.buttonBackgroundColor.setOnClickListener {
            showColorPicker("Pick Background Color", "bg_color") { color ->
                binding.buttonBackgroundColor.apply {
                    peekAlert.setBackgroundColor(color = color)
                    setBackgroundColor(color)
                    text = "Background Color : #${"%06X".format(0xFFFFFF and color)}"
                }
            }
        }

        // Title..

        binding.buttonTitleColor.setOnClickListener {
            showColorPicker("Pick Title Color", "title_color") { color ->
                binding.buttonTitleColor.apply {
                    peekAlert.setTitleColor(color = color)
                    setBackgroundColor(color)
                    text = "Title Color : #${"%06X".format(0xFFFFFF and color)}"
                }
            }
        }

        binding.titleText.addTextChangedListener { edit ->
            edit?.toString()?.let(peekAlert::setTitle)
        }

        binding.titleSize.addTextChangedListener { edit ->
            edit ?: return@addTextChangedListener

            val size = edit.toString().ifBlank { null }?.toFloat() ?: return@addTextChangedListener
            peekAlert.setTitleSize(size)
        }

        // Content..

        binding.buttonContentColor.setOnClickListener {
            showColorPicker("Pick Content Color", "content_color") { color ->
                binding.buttonContentColor.apply {
                    peekAlert.setTextColor(color = color)
                    setBackgroundColor(color)
                    text = "Content Color : #${"%06X".format(0xFFFFFF and color)}"
                }
            }
        }

        binding.contentText.addTextChangedListener { edit ->
            edit?.toString()?.let(peekAlert::setText)
        }

        binding.contentSize.addTextChangedListener { edit ->
            edit ?: return@addTextChangedListener

            val size = edit.toString().ifBlank { null }?.toFloat() ?: return@addTextChangedListener
            peekAlert.setTextSize(size)
        }

        // Icon..

        binding.toggleIconVisibility.setOnCheckedChangeListener { _, isChecked ->
            val (state, icon) = when(isChecked) {
                true  -> "VISIBLE"   to dev.mooner.peekalert.R.drawable.round_info_24
                false -> "INVISIBLE" to null
            }

            peekAlert.setIcon(icon)
            binding.toggleIconVisibility.text = "Icon Visibility : $state"
        }

        binding.buttonIconColor.setOnClickListener {
            showColorPicker("Pick Icon Color", "icon_color") { color ->
                binding.buttonIconColor.apply {
                    peekAlert.setIconTint(color = color)
                    setBackgroundColor(color)
                    text = "Icon Color : #${"%06X".format(0xFFFFFF and color)}"
                }
            }
        }

        // Layout..

        binding.sliderPadding.addOnChangeListener { _, value, _ ->
            peekAlert.setPaddingDp(value.toInt())
        }

        binding.sliderMarginHoriz.addOnChangeListener { _, value, _ ->
            peekAlert.setMargin(horizontal = value.toInt())
        }

        binding.sliderMarginVert.addOnChangeListener { _, value, _ ->
            peekAlert.setMargin(vertical = value.toInt())
        }

        // Behaviors..

        binding.toggleAutoHide.setOnCheckedChangeListener { _, isChecked ->
            peekAlert.setAutoHide(isChecked)
        }

        binding.toggleDraggable.setOnCheckedChangeListener { _, isChecked ->
            if (binding.toggleHideOnTouch.isChecked)
                binding.toggleHideOnTouch.isChecked = false
            peekAlert.setDraggable(isChecked)
        }

        binding.toggleHideOnTouch.setOnCheckedChangeListener { _, isChecked ->
            if (binding.toggleDraggable.isChecked)
                binding.toggleDraggable.isChecked = false
            peekAlert.setHideOnTouch(isChecked)
        }

        binding.toggleShowAction.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                peekAlert.setAction("Action", textColorInt = Color.parseColor("#000000")) {
                    Toast.makeText(this, "Action clicked", Toast.LENGTH_SHORT).show()
                }
            else
                peekAlert.setAction(text = null) {}
        }

        binding.fabShow.setOnClickListener {
            peekAlert.peek()
        }
    }

    private fun showColorPicker(title: String, prefName: String, onColorPicked: (color: Int) -> Unit) {
        ColorPickerDialog.Builder(this)
            .setTitle(title)
            .setPreferenceName(prefName)
            .setPositiveButton("Select", object : ColorEnvelopeListener {
                override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                    val color = envelope.color
                    onColorPicked(color)
                }
            })
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .apply {
                colorPickerView
                colorPickerView.flagView = BubbleFlag(this@MainActivity)
            }
            .show()
            .window!!.setBackgroundDrawableResource(R.drawable.dialog_background)
    }

    private val Number.dp get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()
}