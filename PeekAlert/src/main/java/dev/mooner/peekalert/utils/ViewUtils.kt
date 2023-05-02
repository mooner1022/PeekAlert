package dev.mooner.peekalert.utils

import android.content.res.Resources
import android.graphics.Rect
import android.util.TypedValue
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

internal val Number.dp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()

internal fun Animation.setOnAnimationEndListener(listener: (Animation) -> Unit) {
    this.setAnimationListener(object : AnimationListener {
        override fun onAnimationStart(p0: Animation?) {}

        override fun onAnimationEnd(anim: Animation) =
            listener(anim)

        override fun onAnimationRepeat(p0: Animation?) {}

    })
}

internal fun getStatusBarHeight(decorView: ViewGroup): Int =
    Rect().apply {
        decorView.getWindowVisibleDisplayFrame(this)
    }.top