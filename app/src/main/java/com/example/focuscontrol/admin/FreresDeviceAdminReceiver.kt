package com.example.focuscontrol.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FreresDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "FRERES_ADMIN"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "FreresDeviceAdminReceiver: Device Admin / Owner status enabled")
        DeviceOwnerHelper.applyDeviceOwnerPolicies(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "FreresDeviceAdminReceiver: Device Admin / Owner status disabled")
    }
}
