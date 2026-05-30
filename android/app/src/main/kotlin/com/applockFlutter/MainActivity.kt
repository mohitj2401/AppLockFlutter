@file:Suppress("DEPRECATION")

package com.applockFlutter

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys


class MainActivity: FlutterActivity() {
    private val channel = "flutter.native/helper"
    private var appInfo: List<ApplicationInfo>? = null
    private var lockedAppList: List<ApplicationInfo> = emptyList()
    private var saveAppData: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveAppData = applicationContext.getSharedPreferences("save_app_data", Context.MODE_PRIVATE)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channel).setMethodCallHandler { call, result ->
            when (call.method) {
                "addToLockedApps" -> {
                    val args = call.arguments as HashMap<*, *>
                    val res = showCustomNotification(args)
                    result.success(res)
                }
                "setPasswordInNative" -> {
                    val args = call.arguments as? Map<String, String>
                    val hash = args?.get("hash")
                    val salt = args?.get("salt")
                    if (hash != null && salt != null) {
                        val encryptedPrefs = getEncryptedPrefs(this)
                        val editor = encryptedPrefs.edit()
                        editor.putString("password_hash", hash)
                        editor.putString("password_salt", salt)
                        editor.apply()
                        
                        // Clear plaintext password if it exists
                        saveAppData?.edit()?.remove("password")?.apply()
                        
                        result.success("Success")
                    } else {
                        result.error("INVALID_ARGUMENTS", "Hash or salt is null", null)
                    }
                }
                "checkOverlayPermission" -> {
                    result.success(Settings.canDrawOverlays(this))
                }
                "stopForeground" -> {
                    stopForegroundService()
                    result.success(null)
                }
                "askOverlayPermission" -> {
                    result.success(checkOverlayPermission())
                }
                "askUsageStatsPermission" -> {
                    if (!isAccessGranted()) {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivity(intent)
                    }
                    result.success(isAccessGranted())
                }
                "checkUsagePermission" -> {
                    result.success(isAccessGranted())
                }
                "isIgnoringBatteryOptimizations" -> {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                    result.success(powerManager.isIgnoringBatteryOptimizations(packageName))
                }
                "requestIgnoreBatteryOptimizations" -> {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                    result.success(null)
                }
                "isAdminActive" -> {
                    val mDPM = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val mAdminName = ComponentName(this, DeviceAdmin::class.java)
                    result.success(mDPM.isAdminActive(mAdminName))
                }
                "requestAdmin" -> {
                    val mAdminName = ComponentName(this, DeviceAdmin::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName)
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enabling this prevents the app from being uninstalled.")
                    startActivity(intent)
                    result.success(null)
                }
                "removeAdmin" -> {
                    val mDPM = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val mAdminName = ComponentName(this, DeviceAdmin::class.java)
                    mDPM.removeActiveAdmin(mAdminName)
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    @SuppressLint("CommitPrefEdits", "LaunchActivityFromNotification")
    private fun showCustomNotification(args: HashMap<*, *>): String {
        lockedAppList = emptyList()
        appInfo = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        val arr = args["app_list"] as? ArrayList<Map<String, *>> ?: return "Error: Invalid app_list"

        val newList = mutableListOf<ApplicationInfo>()
        for (element in arr) {
            val packageName = element["package_name"].toString()
            appInfo?.find { it.packageName == packageName }?.let {
                newList.add(it)
            }
        }
        lockedAppList = newList

        val packageData = lockedAppList.map { it.packageName }

        saveAppData?.edit()?.apply {
            remove("app_data")
            putString("app_data", packageData.toString())
            apply()
        }

        startForegroundService()

        return "Success"
    }

    private fun setIfServiceClosed(data: String) {
        saveAppData?.edit()?.apply {
            putString("is_stopped", data)
            apply()
        }
    }

    private fun startForegroundService() {
        if (Settings.canDrawOverlays(this)) {
            setIfServiceClosed("1")
            ContextCompat.startForegroundService(this, Intent(this, ForegroundService::class.java))
        }
    }

    private fun stopForegroundService() {
        setIfServiceClosed("0")
        stopService(Intent(this, ForegroundService::class.java))
    }

    private fun checkOverlayPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(myIntent)
        }
        return Settings.canDrawOverlays(this)
    }

    private fun isAccessGranted(): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid, applicationInfo.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "secure_save_app_data",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
