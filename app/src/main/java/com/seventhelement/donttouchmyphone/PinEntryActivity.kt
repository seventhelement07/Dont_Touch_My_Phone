package com.seventhelement.donttouchmyphone

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.security.MessageDigest

class PinEntryActivity : AppCompatActivity() {

    private lateinit var pinDisplay: TextView
    private lateinit var pinError: TextView
    private lateinit var tvGraceCountdown: TextView
    private val mySharedPreference = MySharedPreference()
    private var currentInput = ""
    private var graceTimer: CountDownTimer? = null

    private val disarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            graceTimer?.cancel()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over and wake the lock screen
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(KEYGUARD_SERVICE) as? KeyguardManager)
                ?.requestDismissKeyguard(this, null)
        }

        setContentView(R.layout.activity_pin_entry)
        pinDisplay = findViewById(R.id.pinDisplay)
        pinError = findViewById(R.id.pinError)
        tvGraceCountdown = findViewById(R.id.tvGraceCountdown)

        val buttons = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )
        buttons.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { deleteDigit() }

        // Block back — must enter PIN to stop the alarm. A plain onBackPressed() override
        // is no longer guaranteed to run once the predictive-back gesture is in play.
        onBackPressedDispatcher.addCallback(this) { /* consume and do nothing */ }

        updateDisplay()

        val graceMs = intent.getLongExtra("grace_ms", 0L)
        if (graceMs > 0) {
            tvGraceCountdown.visibility = View.VISIBLE
            graceTimer = object : CountDownTimer(graceMs, 1000) {
                override fun onTick(ms: Long) {
                    tvGraceCountdown.text = "Alarm starts in ${(ms / 1000) + 1}s…"
                }
                override fun onFinish() {
                    tvGraceCountdown.visibility = View.GONE
                }
            }.start()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(FourgroundService.ACTION_DISARM)
        ContextCompat.registerReceiver(this, disarmReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(disarmReceiver) } catch (_: IllegalArgumentException) {}
    }

    override fun onResume() {
        super.onResume()
        if (!isServiceRunning()) finish()
    }

    private fun appendDigit(digit: String) {
        if (currentInput.length >= 4) return
        currentInput += digit
        updateDisplay()
        if (currentInput.length == 4) checkPin()
    }

    private fun deleteDigit() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        pinDisplay.text = buildString {
            repeat(4) { i ->
                if (i < currentInput.length) append("●") else append("○")
                if (i < 3) append("  ")
            }
        }
    }

    private fun checkPin() {
        val savedHash = mySharedPreference.getPin(this)
        // No PIN on file at all (e.g. cleared by the legacy-hash migration mid-alarm) means
        // there's nothing valid to check against — fail open rather than lock the owner out
        // of their own phone with a PIN that can never match.
        val matches = savedHash == null || hash(currentInput) == savedHash
        if (matches) {
            mySharedPreference.setArmed(this, false)
            stopService(Intent(this, FourgroundService::class.java))
            finish()
        } else {
            pinError.visibility = android.view.View.VISIBLE
            pinDisplay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            currentInput = ""
            updateDisplay()
        }
    }

    override fun onDestroy() {
        graceTimer?.cancel()
        super.onDestroy()
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == FourgroundService::class.java.name }
    }

    private fun hash(pin: String): String {
        val salt = mySharedPreference.getOrCreateSalt(this)
        val bytes = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
