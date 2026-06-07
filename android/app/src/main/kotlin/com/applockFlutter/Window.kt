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
import android.content.pm.ActivityInfo
import android.content.Intent
import android.widget.ImageButton
import androidx.biometric.BiometricManager

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
    private var fingerprintButton: ImageButton? = null
    
    var justUnlocked: Boolean = false

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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL 
            or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        mParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = layoutInflater.inflate(R.layout.pin_activity, null)

        mParams.gravity = Gravity.CENTER
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mPinLockView = mView.findViewById(R.id.pin_lock_view)
        mIndicatorDots = mView.findViewById(R.id.indicator_dots)
        txtView = mView.findViewById(R.id.alertError)
        fingerprintButton = mView.findViewById(R.id.fingerprint_button)
        
        fingerprintButton?.setOnClickListener {
            launchFingerprintActivity()
        }

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
                Log.d("AppLock", "Window added to WindowManager")
                ForegroundService.activeWindow = this
                checkAndLaunchBiometrics()
            }
        } catch (e: Exception) {
            Log.e("AppLock", "Error opening window", e)
        }
    }

    fun forceOpen() {
        try {
            justUnlocked = false
            if (mView.parent != null) {
                mWindowManager.removeView(mView)
                Log.d("AppLock", "Existing window removed for force re-add")
            }
            mWindowManager.addView(mView, mParams)
            Log.d("AppLock", "Window force added to WindowManager")
            ForegroundService.activeWindow = this
            checkAndLaunchBiometrics()
        } catch (e: Exception) {
            Log.e("AppLock", "Error force opening window", e)
        }
    }

    fun isOpen(): Boolean {
        return mView.parent != null
    }
    
    fun wasJustUnlocked(): Boolean {
        val result = justUnlocked
        if (result) {
            justUnlocked = false
            Log.d("AppLock", "Reporting wasJustUnlocked = true")
        }
        return result
    }

    fun close() {
        try {
            if (mView.parent != null) {
                mWindowManager.removeView(mView)
                Log.d("AppLock", "Window removed from WindowManager")
            }
            if (ForegroundService.activeWindow == this) {
                ForegroundService.activeWindow = null
            }
        } catch (e: Exception) {
            Log.e("AppLock", "Error closing window", e)
        }
    }

    private fun isBiometricHardwareAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun checkAndLaunchBiometrics() {
        val saveAppData = context.getSharedPreferences("save_app_data", Context.MODE_PRIVATE)
        val isBiometricEnabled = saveAppData.getBoolean("use_biometric", false)
        
        if (isBiometricEnabled && isBiometricHardwareAvailable(context)) {
            fingerprintButton?.visibility = View.VISIBLE
            launchFingerprintActivity()
        } else {
            fingerprintButton?.visibility = View.GONE
        }
    }

    private fun launchFingerprintActivity() {
        try {
            val intent = Intent(context, FingerprintActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppLock", "Failed to start FingerprintActivity", e)
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
                    Log.d("AppLock", "Correct password entered")
                    failedAttempts = 0
                    txtView!!.visibility = View.GONE
                    justUnlocked = true
                    close()
                } else {
                    Log.d("AppLock", "Invalid password attempt")
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
                Log.d("AppLock", "No password stored, auto-unlocking")
                justUnlocked = true
                close()
            }
        } catch (e: Exception) {
            Log.e("AppLock", "Error in doneButton", e)
        }
    }
}
