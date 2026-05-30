package com.applockFlutter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.*

class ForegroundService : Service() {
    private var timer: Timer = Timer()
    private var timerReload: Long = 500
    private var currentAppActivityList = arrayListOf<String>()
    private lateinit var mHomeWatcher: HomeWatcher

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val channelId = "AppLock-10"
        val channel = NotificationChannel(
            channelId,
            "App Lock Service",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Lock is running")
            .setContentText("Protecting your apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notification)
        
        mHomeWatcher = HomeWatcher(this)
        startMyOwnForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val saveAppData: SharedPreferences = applicationContext.getSharedPreferences("save_app_data", Context.MODE_PRIVATE)
        val isStopped = saveAppData.getString("is_stopped", "0")
        if (isStopped == "1") {
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            restartServiceIntent.setPackage(packageName)
            startService(restartServiceIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startMyOwnForeground() {
        val window = Window(this)
        mHomeWatcher.setOnHomePressedListener(object : HomeWatcher.OnHomePressedListener {
            override fun onHomePressed() {
                currentAppActivityList.clear()
                if (window.isOpen()) {
                    window.close()
                }
            }
            override fun onHomeLongPressed() {
                currentAppActivityList.clear()
                if (window.isOpen()) {
                    window.close()
                }
            }
        })
        mHomeWatcher.startWatch()
        timerRun(window)
    }

    override fun onDestroy() {
        timer.cancel()
        mHomeWatcher.stopWatch()
        super.onDestroy()
    }

    private fun timerRun(window: Window) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                isServiceRunning(window)
            }
        }, 0, timerReload)
    }

    fun isServiceRunning(window: Window) {
        val saveAppData: SharedPreferences = applicationContext.getSharedPreferences("save_app_data", Context.MODE_PRIVATE)
        val appData = saveAppData.getString("app_data", "") ?: ""
        val lockedAppList = appData.replace("[", "").replace("]", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (lockedAppList.isEmpty()) return

        val mUsageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        val usageEvents = mUsageStatsManager.queryEvents(time - timerReload, time)
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkgName = event.packageName ?: continue
            
            if (lockedAppList.contains(pkgName)) {
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (currentAppActivityList.isEmpty()) {
                        currentAppActivityList.add(event.className ?: "")
                        Handler(Looper.getMainLooper()).post {
                            window.open()
                        }
                        break
                    } else if (!currentAppActivityList.contains(event.className)) {
                        currentAppActivityList.add(event.className ?: "")
                    }
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    currentAppActivityList.remove(event.className)
                }
            }
        }
    }
}
