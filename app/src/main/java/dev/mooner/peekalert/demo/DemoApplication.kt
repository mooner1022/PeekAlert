package dev.mooner.peekalert.demo

import android.app.Application
import com.google.android.material.color.DynamicColors

class DemoApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}