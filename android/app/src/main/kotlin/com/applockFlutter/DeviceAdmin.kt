package com.applockFlutter

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Uninstall Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Uninstall Protection Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        val serviceIntent = Intent(context, ForegroundService::class.java)
        serviceIntent.action = "ACTION_FORCE_LOCK"
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        return "You must enter your AppLock password to disable Uninstall Protection."
    }
}
