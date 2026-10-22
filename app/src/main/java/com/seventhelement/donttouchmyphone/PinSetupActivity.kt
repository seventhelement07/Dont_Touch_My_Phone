package com.seventhelement.donttouchmyphone

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class PinSetupActivity : AppCompatActivity() {

    private lateinit var pinDisplay: TextView
    private lateinit var pinError: TextView
    private lateinit var subtitle: TextView

    private val mySharedPreference = MySharedPreference()
    private var currentInput = ""
    private var firstPin = ""
    private var isConfirming = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        pinDisplay = findViewById(R.id.pinDisplay)
        pinError = findViewById(R.id.pinError)
        subtitle = findViewById(R.id.pinSetupSubtitle)

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

        // Block back navigation (including the predictive-back gesture) — PIN must be set.
        // Plain onBackPressed() override is no longer guaranteed to be called once the
        // system handles back via the predictive-back callback path.
        onBackPressedDispatcher.addCallback(this) { /* consume and do nothing */ }

        updateDisplay()
    }

    private fun appendDigit(digit: String) {
        if (currentInput.length >= 4) return
        currentInput += digit
        updateDisplay()
        if (currentInput.length == 4) onPinComplete()
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

    private fun onPinComplete() {
        if (!isConfirming) {
            firstPin = currentInput
            isConfirming = true
            currentInput = ""
            subtitle.text = "Confirm your PIN"
            pinError.visibility = android.view.View.INVISIBLE
            updateDisplay()
        } else {
            if (currentInput == firstPin) {
                mySharedPreference.savePin(this, hash(currentInput))
                Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                pinError.text = "PINs don't match — try again"
                pinError.visibility = android.view.View.VISIBLE
                pinDisplay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                isConfirming = false
                firstPin = ""
                currentInput = ""
                subtitle.text = "Enter a 4-digit PIN to stop the alarm"
                updateDisplay()
            }
        }
    }

    private fun hash(pin: String): String {
        val salt = mySharedPreference.getOrCreateSalt(this)
        val bytes = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
