package dev.mooner.peekalert.utils

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.TypedValue

internal val Number.dp get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()

internal val mainHandler = Handler(Looper.getMainLooper())

/** Runs [block] on the main thread: at once when already there, as views may only be touched from it. */
internal inline fun onMainThread(crossinline block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper())
        block()
    else
        mainHandler.post { block() }
}
