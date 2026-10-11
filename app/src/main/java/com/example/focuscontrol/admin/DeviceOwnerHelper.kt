package com.example.focuscontrol.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

enum class DeviceOwnerStatus {
    NOT_PROVISIONED,              // Normal installation mode (Uninstall protection inactive)
    PROVISIONED_POLICY_PENDING,   // Device Owner confirmed, but policy application pending/unconfirmed
    ENFORCED                      // Device Owner confirmed AND setUninstallBlocked() successfully enforced
}

object DeviceOwnerHelper {
    private const val TAG = "FRERES_DEVICE_OWNER"

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, FreresDeviceAdminReceiver::class.java)
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isProfileOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isProfileOwnerApp(context.packageName)
    }

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(getAdminComponent(context))
    }

    fun setUninstallProtection(context: Context, blockUninstall: Boolean): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = getAdminComponent(context)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Cannot set uninstall protection: FRERES is running in normal installation mode.")
            return false
        }

        return try {
            dpm.setUninstallBlocked(admin, context.packageName, blockUninstall)
            Log.i(TAG, "Uninstall protection policy set to $blockUninstall for ${context.packageName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set uninstall protection policy", e)
            false
        }
    }

    fun isUninstallBlocked(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = getAdminComponent(context)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return false
        return try {
            dpm.isUninstallBlocked(admin, context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceOwnerStatus(context: Context): DeviceOwnerStatus {
        if (!isDeviceOwner(context)) {
            return DeviceOwnerStatus.NOT_PROVISIONED
        }
        val isBlocked = isUninstallBlocked(context)
        if (isBlocked) {
            return DeviceOwnerStatus.ENFORCED
        }
        val applied = setUninstallProtection(context, true)
        return if (applied && isUninstallBlocked(context)) {
            DeviceOwnerStatus.ENFORCED
        } else {
            DeviceOwnerStatus.PROVISIONED_POLICY_PENDING
        }
    }

    fun applyDeviceOwnerPolicies(context: Context): Boolean {
        if (!isDeviceOwner(context)) {
            Log.d(TAG, "Skipping Device Owner policies: App running in normal installation mode.")
            return false
        }
        val success = setUninstallProtection(context, true)
        Log.i(TAG, "Applied Device Owner uninstall protection policy. Result: $success")
        return success
    }
}
