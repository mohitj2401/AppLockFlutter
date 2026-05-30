package com.applockFlutter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class HomeWatcher(private val mContext: Context) {
    private val mFilter: IntentFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
    private var mListener: OnHomePressedListener? = null
    private var mReceiver: InnerReceiver? = null

    fun setOnHomePressedListener(listener: OnHomePressedListener?) {
        mListener = listener
        mReceiver = InnerReceiver()
    }

    fun startWatch() {
        if (mReceiver != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(mReceiver, mFilter, Context.RECEIVER_EXPORTED)
            } else {
                mContext.registerReceiver(mReceiver, mFilter)
            }
        }
    }

    fun stopWatch() {
        if (mReceiver != null) {
            try {
                mContext.unregisterReceiver(mReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
    }

    interface OnHomePressedListener {
        fun onHomePressed()
        fun onHomeLongPressed()
    }

    inner class InnerReceiver : BroadcastReceiver() {
        private val SYSTEM_DIALOG_REASON_KEY = "reason"
        private val SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps"
        private val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"
        
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
                if (reason != null) {
                    if (mListener != null) {
                        if (reason == SYSTEM_DIALOG_REASON_HOME_KEY) {
                            mListener!!.onHomePressed()
                        } else if (reason == SYSTEM_DIALOG_REASON_RECENT_APPS) {
                            mListener!!.onHomeLongPressed()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "HomeWatcher"
    }
}
