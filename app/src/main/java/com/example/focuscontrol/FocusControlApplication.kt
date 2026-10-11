package com.example.focuscontrol

import android.app.Activity
import android.app.Application
import android.os.Bundle

class FocusControlApplication : Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        private var startedActivities = 0

        val isAppInForeground: Boolean
            get() = startedActivities > 0
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities < 0) {
            startedActivities = 0
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
