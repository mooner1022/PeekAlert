/**
 * PeekAlert created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the MIT License.
 *
 * @author mooner1022
 */

package dev.mooner.peekalert

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import dev.mooner.peekalert.interpolator.CubicBezierInterpolator
import dev.mooner.peekalert.utils.dp
import dev.mooner.peekalert.utils.getStatusBarHeight
import dev.mooner.peekalert.utils.setOnAnimationEndListener
import java.lang.ref.WeakReference

typealias PeekActionListener = (view: View) -> Unit

class PeekAlert internal constructor(
    private val parentView: ViewGroup,
    private val isMultiWindow: Boolean
) {
    private var instance: PeekAlertView? = null

    private var position   : Position = Position.Bottom
    private var radius     : Float? = null
    private var width      : Int = WRAP_CONTENT
    private var bgColor    : Int? = null
    private var paddingPx  : Int? = null
    private var margin     : Pair<Int?, Int?> = Pair(null, null)
    private val titleConfig: TextConfig by lazy { TextConfig() }
    private val textConfig : TextConfig by lazy { TextConfig() }
    private var iconRes    : Int? = R.drawable.round_info_24
    private var iconTint   : Int? = null
    private var draggable  : Boolean = false
    private var hideOnTouch: Boolean = false
    private var autoHideMillis: Long? = null
    private val actionConfig  : ActionConfig by lazy { ActionConfig() }

    /**
     * Set the position of alert to be shown
     * @param position : Position of alert
     * @see Position
     */
    fun setPosition(position: Position): PeekAlert {
        this.position = position
        return this
    }

    /**
     * Set the background color of alert
     * @param res : Color resource
     * @param color : Color int
     */
    @JvmOverloads
    fun setBackgroundColor(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        this.bgColor = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set corner radius of alert
     * @param radius : radius of corner, by px
     */
    fun setCornerRadius(@Px radius: Float): PeekAlert {
        this.radius = radius
        return this
    }

    /**
     * Set width of alert view
     * Should be set as one of MATCH_PARENT or WRAP_CONTENT, unless you have a particular purpose
     * @param width : width of view
     */
    fun setWidth(width: Int): PeekAlert {
        this.width = width
        return this
    }

    /**
     * Set title of alert
     * @param title : The desired content of title text
     */
    fun setTitle(title: String): PeekAlert {
        titleConfig.content = title
        return this
    }

    /**
     * Set size of title text
     * @param size : The scaled pixel size
     */
    fun setTitleSize(size: Float): PeekAlert {
        titleConfig.size = size
        return this
    }

    /**
     * Set color of title text
     * @param res : Resource value of color
     * @param color : Int value of color
     */
    @JvmOverloads
    fun setTitleColor(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        titleConfig.color = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set typeface of title text
     * @param typeface : The desired typeface of title
     */
    fun setTitleTypeface(typeface: Typeface): PeekAlert {
        titleConfig.typeface = typeface
        return this
    }

    /**
     * Set text of alert
     * @param text : The desired content of text
     */
    fun setText(text: String): PeekAlert {
        textConfig.content = text
        return this
    }

    /**
     * Set size of text
     * @param size : The scaled pixel size
     */
    fun setTextSize(size: Float): PeekAlert {
        textConfig.size = size
        return this
    }

    /**
     * Set color of text
     * @param res : Resource value of color
     * @param color : Int value of color
     */
    @JvmOverloads
    fun setTextColor(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        textConfig.color = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set typeface of text
     * @param typeface : The desired typeface of text
     */
    fun setTextTypeface(typeface: Typeface): PeekAlert {
        textConfig.typeface = typeface
        return this
    }

    /**
     * Set icon of alert
     * @param res : Drawable resource of desired icon content
     */
    fun setIcon(@DrawableRes res: Int?): PeekAlert {
        this.iconRes = res
        return this
    }

    /**
     * Set tint color of icon
     * @param res : Resource value of color
     * @param color : Int value of color
     */
    @JvmOverloads
    fun setIconTint(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        this.iconTint = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set padding of alert, in px
     * @param px : The desired padding in px
     */
    fun setPaddingPx(@Px px: Int): PeekAlert {
        this.paddingPx = px
        return this
    }

    /**
     * Set padding of alert, in dp
     * @param dp : The desired padding in dp
     */
    fun setPaddingDp(dp: Int): PeekAlert =
        setPaddingPx(dp.dp)

    /**
     * Set horizontal, vertical margin of alert
     * @param horizontal : The desired horizontal margin of alert, in px
     * @param vertical : The desired vertical margin of alert, in px
     */
    fun setMargin(
        @Dimension(unit = Dimension.DP) horizontal: Int? = null,
        @Dimension(unit = Dimension.DP) vertical: Int? = null
    ): PeekAlert {
        margin = Pair(horizontal ?: margin.first, vertical ?: margin.second)
        return this
    }

    /**
     * Set alert to be dragged and dismissed by user
     */
    fun setDraggable(draggable: Boolean): PeekAlert {
        this.draggable = draggable
        return this
    }

    /**
     * Set alert to be hidden on user touch
     */
    fun setHideOnTouch(hide: Boolean): PeekAlert {
        this.hideOnTouch = hide
        return this
    }

    /**
     * Set alert to be automatically hidden after 3000ms
     * @see setAutoHide(Long)
     */
    fun setAutoHide(hide: Boolean): PeekAlert {
        this.autoHideMillis =
            if (hide)
                3000L
            else
                null
        return this
    }

    /**
     * Set alert to be automatically hidden after the given delay
     * @param delay : The desired delay in milliseconds
     * @see setAutoHide(Boolean)
     */
    fun setAutoHide(delay: Long = 3000L): PeekAlert {
        this.autoHideMillis = delay
        return this
    }

    /**
     * Set a user-clickable button shown at right-end of alert
     * @param config : The desired action config
     * @see setAction
     */
    fun setAction(config: ActionConfig): PeekAlert {
        actionConfig.text             = config.text
        actionConfig.backgroundColor  = config.backgroundColor
        actionConfig.textColor        = config.textColor
        actionConfig.onActionListener = config.onActionListener
        return this
    }

    /**
     * Set a user-clickable button shown at right-end of alert
     * @param text : The desired text of action button
     * @param backgroundColorRes : The desired background color of action, as Resource value
     * @param backgroundColorInt : The desired background color of action, as Color Int value
     * @param textColorRes : The desired text color of action, as Resource value
     * @param textColorInt : The desired text color of action, as Color Int value
     * @param onClick : Callback listener called on user click action
     * @see setAction
     */
    @JvmOverloads
    fun setAction(
        text: String?,
        @ColorRes
        backgroundColorRes: Int? = null,
        @ColorInt
        backgroundColorInt: Int? = null,
        @ColorRes
        textColorRes: Int? = null,
        @ColorInt
        textColorInt: Int? = null,
        onClick: PeekActionListener
    ): PeekAlert {
        setAction(
            ActionConfig(
            text = text,
            backgroundColor = backgroundColorRes
                ?.let(parentView.context::getColor)
                ?: backgroundColorInt,
            textColor = textColorRes
                ?.let(parentView.context::getColor)
                ?: textColorInt,
            onActionListener = onClick
        ))
        return this
    }

    /**
     * Show the alert on screen, with settings set before
     * @see hide
     */
    fun peek() {
        removeViewFromParent()

        val context = parentView.context
        val peek = PeekAlertView(context, width).apply {
            id = getIdByPosition()
        }

        val (marginHoriz, marginVert) = margin
        val padding = marginVert ?: 16.dp
        val (padTop, padBottom) = run {
            var top    = 0
            var bottom = 0
            when(position) {
                Position.Top -> {
                    top = if (!isMultiWindow)
                        padding + getStatusBarHeight(parentView)
                    else
                        padding
                }
                Position.Bottom -> {
                    bottom = padding
                }
            }
            top to bottom
        }
        peek.setPadding(0, padTop, 0, padBottom)

        if (titleConfig.content != null)
            peek.setTitle(titleConfig)

        if (textConfig.content != null)
            peek.setText(textConfig)

        peek.setIcon(iconRes, iconTint)

        if (actionConfig.text != null)
            peek.setAction(actionConfig)

        peek.setDraggable(draggable, position)

        radius?.let(peek::setRadius)

        bgColor?.let(peek::setBackgroundColor)

        paddingPx?.let(peek::setPadding)

        marginHoriz?.let(peek::setMargin)

        if (position == Position.Bottom) {
            val rootView = parentView.findViewById<FrameLayout>(android.R.id.content)
            val params = FrameLayout.LayoutParams(width, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER or Gravity.BOTTOM
            }
            rootView.addView(peek, params)
        } else {
            parentView.addView(peek)
        }

        AnimationUtils.loadAnimation(context, getAnimationByPosition(true))
            .apply {
                interpolator = CubicBezierInterpolator(0.0, 0.0, 0.05, 1.0)
                duration = 300
            }
            .let(peek::startAnimation)

        instance = peek
        if (autoHideMillis != null) {
            peek.postDelayed(autoHideMillis!!) {
                hide(peek)
            }
        }
        if (hideOnTouch) {
            peek.setOnClickListener {
                println("click")
                hide(peek)
            }
        }
    }

    /**
     * Hide the alert currently being shown on the screen if present
     * @see peek
     */
    fun hide() {
        if (instance != null)
            hide(instance!!)
    }

    private fun hide(view: ViewGroup) {
        AnimationUtils.loadAnimation(view.context, getAnimationByPosition(false))
            .apply {
                interpolator = CubicBezierInterpolator(0.05, 1.0, 1.0, 1.0)
                setOnAnimationEndListener {
                    removeViewFromParent()
                    instance = null
                }
            }
            .let(view::startAnimation)
    }

    private fun removeViewFromParent() {
        getParent()?.let { parent ->
            val id = getIdByPosition()
            parent.findViewById<ViewGroup>(id)?.let {
                parent.removeView(it)
            }
        }
    }

    private fun getParent(): ViewGroup? =
        when(position) {
            Position.Top ->
                parentView
            Position.Bottom ->
                parentView.findViewById(android.R.id.content)
        }

    private fun getAnimationByPosition(show: Boolean) =
        when(position) {
            Position.Top ->
                if (show) R.anim.top_show else R.anim.top_hide
            Position.Bottom ->
                if (show) R.anim.bottom_show else R.anim.bottom_hide
        }

    private fun getIdByPosition() =
        when(position) {
            Position.Top ->
                R.id.id_alert_view_top
            Position.Bottom ->
                R.id.id_alert_view_bottom
        }

    sealed class Position {
        object Top   : Position()
        object Bottom: Position()
    }

    companion object {

        @JvmStatic
        fun create(view: ViewGroup): PeekAlert =
            PeekAlert(view, false)

        @JvmStatic
        fun create(activity: Activity): PeekAlert =
            PeekAlert(
                activity.window.decorView as ViewGroup,
                activity.isInMultiWindowMode
            )

        @JvmStatic
        fun create(fragment: Fragment): PeekAlert =
            create(fragment.requireActivity())
    }
}