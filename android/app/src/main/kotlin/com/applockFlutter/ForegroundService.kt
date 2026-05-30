package com.applockFlutter

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import java.util.*

class ForegroundService : Service() {
    private var timer: Timer = Timer()
    private var timerReload: Long = 100 // Reduced from 500ms to 100ms for faster detection
    private var currentAppActivityList = arrayListOf<String>()
    private lateinit var mHomeWatcher: HomeWatcher
    private var cachedLockedAppList: List<String> = emptyList()
    private lateinit var saveAppData: SharedPreferences

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        saveAppData = applicationContext.getSharedPreferences("save_app_data", Context.MODE_PRIVATE)
        updateLockedAppCache()

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
            .setOngoing(true)
            .build()
        startForeground(1, notification)
        
        mHomeWatcher = HomeWatcher(this)
        startMyOwnForeground()
    }

    private fun updateLockedAppCache() {
        val appData = saveAppData.getString("app_data", "") ?: ""
        cachedLockedAppList = appData.replace("[", "").replace("]", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateLockedAppCache() // Refresh cache when service is started/restarted
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isStopped = saveAppData.getString("is_stopped", "0")
        
        if (isStopped == "1") {
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            restartServiceIntent.setPackage(packageName)
            
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 
                1, 
                restartServiceIntent, 
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME, 
                SystemClock.elapsedRealtime() + 1000, 
                restartServicePendingIntent
            )
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
        // Use cached list to avoid repeated SharedPreferences IO
        if (cachedLockedAppList.isEmpty()) {
            updateLockedAppCache()
            if (cachedLockedAppList.isEmpty()) return
        }

        val mUsageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        // We use a slightly larger window (1000ms) to ensure we don't miss any events 
        // that Android might report with a slight delay.
        val usageEvents = mUsageStatsManager.queryEvents(time - 1000, time)
        val event = UsageEvents.Event()

        var lastEvent: UsageEvents.Event? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            // We only care about ACTIVITY_RESUMED events for locking
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastEvent = UsageEvents.Event(event)
            }
        }

        if (lastEvent != null) {
            val pkgName = lastEvent.packageName
            if (cachedLockedAppList.contains(pkgName)) {
                if (!window.isOpen()) {
                    Handler(Looper.getMainLooper()).post {
                        window.open()
                    }
                }
            } else {
                // If the app is NOT in the locked list, and it's NOT our own app, close the window
                // (Assuming "com.applockFlutter" is your package name)
                if (pkgName != packageName && window.isOpen()) {
                    Handler(Looper.getMainLooper()).post {
                        // Optional: only close if user has successfully unlocked 
                        // For now, keeping it simple
                    }
                }
            }
        }
    }
}
