package com.applockFlutter

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.andrognito.pinlockview.PinLockView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

@SuppressLint("InflateParams")
class Window(private val context: Context) {
    private val mView: View
    var pinCode: String = ""
    var txtView: TextView? = null
    private var mParams: WindowManager.LayoutParams
    private val mWindowManager: WindowManager
    private val layoutInflater: LayoutInflater

    private var mPinLockView: PinLockView? = null
    private var mIndicatorDots: IndicatorDots? = null
    
    private var justUnlocked: Boolean = false

    private val mPinLockListener: PinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) {
            pinCode = pin
            doneButton()
        }

        override fun onEmpty() {
        }

        override fun onPinChange(pinLength: Int, intermediatePin: String) {}
    }

    init {
        mParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = layoutInflater.inflate(R.layout.pin_activity, null)

        mParams.gravity = Gravity.CENTER
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mPinLockView = mView.findViewById(R.id.pin_lock_view)
        mIndicatorDots = mView.findViewById(R.id.indicator_dots)
        txtView = mView.findViewById(R.id.alertError)

        mPinLockView!!.attachIndicatorDots(mIndicatorDots)
        mPinLockView!!.setPinLockListener(mPinLockListener)
        mPinLockView!!.pinLength = 6
        mPinLockView!!.textColor = ContextCompat.getColor(context, android.R.color.white)
        mIndicatorDots!!.indicatorType = IndicatorDots.IndicatorType.FILL_WITH_ANIMATION
    }

    fun open() {
        try {
            if (mView.parent == null) {
                justUnlocked = false
                mWindowManager.addView(mView, mParams)
            }
        } catch (e: Exception) {
            Log.e("Window", "Error opening window", e)
        }
    }

    fun isOpen(): Boolean {
        return mView.parent != null
    }
    
    fun wasJustUnlocked(): Boolean {
        val result = justUnlocked
        justUnlocked = false
        return result
    }

    fun close() {
        try {
            if (mView.parent != null) {
                mWindowManager.removeView(mView)
            }
        } catch (e: Exception) {
            Log.e("Window", "Error closing window", e)
        }
    }

    private var failedAttempts = 0
    private var isLockedOut = false
    private val lockoutHandler = Handler(Looper.getMainLooper())

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.fold("") { str, it -> str + "%02x".format(it) }
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

    fun doneButton() {
        try {
            mPinLockView!!.resetPinLockView()
            if (isLockedOut) {
                txtView!!.text = "Too many failed attempts. Locked out."
                txtView!!.visibility = View.VISIBLE
                return
            }

            val encryptedPrefs = getEncryptedPrefs(context)
            val storedHash = encryptedPrefs.getString("password_hash", null)
            val salt = encryptedPrefs.getString("password_salt", null)

            if (storedHash != null && salt != null) {
                val hashedEntered = sha256(pinCode + salt)
                if (hashedEntered == storedHash) {
                    failedAttempts = 0
                    txtView!!.visibility = View.GONE
                    justUnlocked = true
                    close()
                } else {
                    failedAttempts++
                    if (failedAttempts >= 5) {
                        isLockedOut = true
                        mPinLockView!!.isEnabled = false
                        txtView!!.text = "Locked out for 30s."
                        txtView!!.visibility = View.VISIBLE
                        lockoutHandler.postDelayed({
                            isLockedOut = false
                            failedAttempts = 0
                            mPinLockView!!.isEnabled = true
                            txtView!!.visibility = View.GONE
                        }, 30000)
                    } else {
                        txtView!!.text = "Invalid passcode. Attempts: $failedAttempts/5"
                        txtView!!.visibility = View.VISIBLE
                    }
                }
            } else {
                justUnlocked = true
                close()
            }
        } catch (e: Exception) {
            Log.e("Window", "Error in doneButton", e)
        }
    }
}
