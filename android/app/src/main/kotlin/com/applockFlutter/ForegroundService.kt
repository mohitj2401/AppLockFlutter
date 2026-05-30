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
    private var timerReload: Long = 100 
    private lateinit var mHomeWatcher: HomeWatcher
    private var cachedLockedAppList: List<String> = emptyList()
    private lateinit var saveAppData: SharedPreferences
    
    private var lastProcessedEventTime: Long = System.currentTimeMillis()
    private var currentlyUnlockedPackage: String? = null
    private var lastResumedPackage: String? = null

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
        updateLockedAppCache()
        lastProcessedEventTime = System.currentTimeMillis()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isStopped = saveAppData.getString("is_stopped", "0")
        if (isStopped == "1") {
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            restartServiceIntent.setPackage(packageName)
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 1, restartServiceIntent, 
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startMyOwnForeground() {
        val window = Window(this)
        mHomeWatcher.setOnHomePressedListener(object : HomeWatcher.OnHomePressedListener {
            override fun onHomePressed() {
                currentlyUnlockedPackage = null
                if (window.isOpen()) window.close()
            }
            override fun onHomeLongPressed() {
                currentlyUnlockedPackage = null
                if (window.isOpen()) window.close()
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
        if (cachedLockedAppList.isEmpty()) {
            updateLockedAppCache()
            if (cachedLockedAppList.isEmpty()) return
        }

        val mUsageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val usageEvents = mUsageStatsManager.queryEvents(lastProcessedEventTime, time)
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkgName = event.packageName
                val className = event.className ?: ""
                
                lastResumedPackage = pkgName
                lastProcessedEventTime = event.timeStamp

                // Improved sensitive page detection
                val isSensitivePage = className.lowercase().contains("deviceadmin") || 
                                     className.lowercase().contains("devicepolicy")

                if (cachedLockedAppList.contains(pkgName)) {
                    // Lock if package changed OR it's a sensitive page within an unlocked package
                    if (pkgName != currentlyUnlockedPackage || isSensitivePage) {
                        if (!window.isOpen()) {
                            Handler(Looper.getMainLooper()).post {
                                window.open()
                            }
                        }
                    }
                } else if (pkgName != packageName) {
                    if (window.isOpen()) {
                        Handler(Looper.getMainLooper()).post { window.close() }
                    }
                }
            }
        }

        if (window.wasJustUnlocked()) {
            currentlyUnlockedPackage = lastResumedPackage
        }
    }
}
