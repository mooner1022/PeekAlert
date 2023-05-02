package dev.mooner.peekalert

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.*
import dev.mooner.peekalert.databinding.LayoutAlertBinding
import dev.mooner.peekalert.utils.dp


class PeekAlertView(
    context: Context,
    width: Int
): ConstraintLayout(context) {

    constructor(context: Context):
            this(context, WRAP_CONTENT)

    private val binding: LayoutAlertBinding

    // Used for swipe action
    private var orgPos: Float = 0f
    private var touchDownPos: Float = 0f

    fun setTitle(config: TextConfig) {
        binding.title.apply {
            visibility = View.VISIBLE
            text = config.content
            config.size?.let(::setTextSize)
            config.color?.let(::setTextColor)
            config.typeface?.let(::setTypeface)
        }
    }

    fun setText(config: TextConfig) {
        binding.content.apply {
            text = config.content
            config.size?.let(::setTextSize)
            config.color?.let(::setTextColor)
            config.typeface?.let(::setTypeface)
        }
    }

    fun setIcon(@DrawableRes res: Int?, @ColorInt tint: Int?) {
        binding.icon.apply {
            if (res != null) {
                visibility = VISIBLE
                updateLayoutParams {
                    width = WRAP_CONTENT
                }
                setImageResource(res)
                if (tint != null) {
                    imageTintList = ColorStateList.valueOf(tint)
                }
            } else {
                visibility = INVISIBLE
                updateLayoutParams {
                    width = 16.dp
                }
            }
        }
    }

    override fun setBackgroundColor(@ColorInt color: Int) {
        binding.cardBackground.setCardBackgroundColor(color)
    }

    fun setPadding(@Px px: Int) {
        binding.icon.setPadding(px)
        binding.cardAction.apply {
            if (isVisible) {
                updateLayoutParams<LinearLayout.LayoutParams> {
                    marginEnd = px
                }
            }
        }
        binding.content.updateLayoutParams<LinearLayout.LayoutParams> {
            marginEnd = px
        }
    }

    fun setMargin(horizontal: Int) {
        binding.root.updatePadding(left = horizontal, right = horizontal)
    }

    fun setRadius(@Px radius: Float) {
        binding.cardBackground.radius = radius
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDraggable(draggable: Boolean, position: PeekAlert.Position) {
        if (draggable) {
            when (position) {
                PeekAlert.Position.Top ->
                    binding.root.setOnTouchListener(topSwipeListener)
                PeekAlert.Position.Bottom ->
                    binding.root.setOnTouchListener(bottomSwipeListener)
            }
        }
    }

    fun setAction(config: ActionConfig) {
        binding.cardAction.apply {
            visibility = View.VISIBLE
            config.backgroundColor?.let(::setCardBackgroundColor)
            setOnClickListener(config.onActionListener)
        }
        binding.actionText.apply {
            text = config.text
            config.textColor?.let(::setTextColor)
        }
    }

    private val topSwipeListener = object : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val touchY = event.rawY

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    orgPos = this@PeekAlertView.y
                    touchDownPos = touchY
                }
                MotionEvent.ACTION_MOVE -> {
                    val isSwipingDown = touchDownPos > touchY
                    if (!isSwipingDown)
                        return false

                    this@PeekAlertView.y = orgPos + (touchY - touchDownPos)
                }
                MotionEvent.ACTION_UP -> {
                    if (touchDownPos - touchY > height / 2) {
                        ObjectAnimator.ofFloat(this@PeekAlertView, "translationY", -height.toFloat()).apply {
                            duration = 100
                            doOnEnd {
                                if (parent != null && parent is ViewGroup) {
                                    (parent as ViewGroup).removeView(this@PeekAlertView)
                                }
                            }
                            start()
                        }
                    } else {
                        ObjectAnimator.ofFloat(this@PeekAlertView, "translationY", 0f).apply {
                            duration = 300
                            start()
                        }
                    }
                }
            }
            return true
        }
    }

    private val bottomSwipeListener = object : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val touchY = event.rawY

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    orgPos = this@PeekAlertView.y
                    touchDownPos = touchY
                }
                MotionEvent.ACTION_MOVE -> {
                    val isSwipingDown = touchDownPos < touchY
                    if (!isSwipingDown)
                        return false

                    this@PeekAlertView.y = orgPos - (touchDownPos - touchY)
                }
                MotionEvent.ACTION_UP -> {

                    if (touchY - touchDownPos > height / 2) {
                        ObjectAnimator.ofFloat(this@PeekAlertView, "translationY", height.toFloat()).apply {
                            duration = 100
                            doOnEnd {
                                if (parent != null && parent is ViewGroup) {
                                    (parent as ViewGroup).removeView(this@PeekAlertView)
                                }
                            }
                            start()
                        }
                    } else {
                        ObjectAnimator.ofFloat(this@PeekAlertView, "translationY", 0f).apply {
                            duration = 300
                            start()
                        }
                    }
                }
            }
            return true
        }
    }

    init {
        binding = LayoutAlertBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        if (width != WRAP_CONTENT) {
            binding.cardBackground.updateLayoutParams {
                this.width = width
            }
        }
    }
}