package com.seventhelement.donttouchmyphone

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.play.core.review.ReviewManagerFactory
import com.seventhelement.donttouchmyphone.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServiceOn = false
    private var cdt: CountDownTimer? = null
    private val mySharedPreference = MySharedPreference()
    private var selectedPosition = -1

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && Build.VERSION.SDK_INT >= 33) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showRationaleDialog(
                        "Notification Permission",
                        "Notifications are needed to show the alarm status while the service runs.",
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    showSettingsDialog("Notification Permission",
                        "Please enable notification permission in Settings.")
                }
            }
        }

    private val pinSetupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                // User didn't set a PIN — close the app
                finish()
            }
        }

    private val onboardingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            mySharedPreference.setOnboardingDone(this)
            // After onboarding, prompt PIN setup if not done yet
            if (!mySharedPreference.hasPin(this)) {
                pinSetupLauncher.launch(Intent(this, PinSetupActivity::class.java))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Don't Touch My Phone"

        // Request permissions
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // A PIN saved before salted hashing was introduced can never match again — clear it
        // so the check below correctly detects "no PIN" and prompts the user to set a new one.
        mySharedPreference.migrateLegacyPinIfNeeded(this)

        // First launch: show onboarding then PIN setup in sequence
        if (mySharedPreference.isFirstLaunch(this)) {
            onboardingLauncher.launch(Intent(this, OnboardingActivity::class.java))
        } else if (!mySharedPreference.hasPin(this)) {
            pinSetupLauncher.launch(Intent(this, PinSetupActivity::class.java))
        }

        // Restore selected sound
        selectedPosition = mySharedPreference.getInt(this).let { if (it == -1) 0 else it }
        updateSoundLabel()

        // Open sound picker bottom sheet
        binding.soundPickerRow.setOnClickListener {
            SoundPickerBottomSheet(selectedPosition) { position ->
                selectedPosition = position
                mySharedPreference.saveInt(this, position)
                updateSoundLabel()
            }.show(supportFragmentManager, "SoundPicker")
        }

        // Sensitivity slider
        val savedIndex = mySharedPreference.getSensitivityIndex(this)
        binding.sensitivitySlider.value = savedIndex.toFloat()
        updateSensitivityHint(savedIndex)
        binding.sensitivitySlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                val index = slider.value.toInt()
                mySharedPreference.saveSensitivityIndex(this@MainActivity, index)
                updateSensitivityHint(index)
            }
        })

        // History button
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, AlarmHistoryActivity::class.java))
        }

        // Service state
        isServiceOn = isServiceRunning(applicationContext, FourgroundService::class.java)

        // Resume protection if it was armed before a reboot but couldn't auto-start from
        // BOOT_COMPLETED directly (Android 15+ restriction) — this is a safe, user-initiated
        // start since the app is now genuinely in the foreground.
        if (!isServiceOn && mySharedPreference.isArmed(this)) {
            val position = mySharedPreference.getInt(this).let { if (it == -1) 0 else it }
            val soundTitle = Constants.getlist().getOrNull(position)?.title ?: ""
            val soundResId = resources.getIdentifier(soundTitle, "raw", packageName)
            val resumeIntent = Intent(applicationContext, FourgroundService::class.java)
                .putExtra("message_key2", soundTitle)
                .putExtra("sound_res_id", soundResId)
            startForegroundService(resumeIntent)
            isServiceOn = true
        }

        applyServiceState(isServiceOn)

        // Start/Stop button
        val serviceIntent = Intent(applicationContext, FourgroundService::class.java)
        binding.image.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(this, "Please select a sound first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val soundTitle = Constants.getlist()[selectedPosition].title
            val soundResId = resources.getIdentifier(soundTitle, "raw", packageName)
            serviceIntent.putExtra("message_key2", soundTitle)
            serviceIntent.putExtra("sound_res_id", soundResId)

            if (isServiceOn) {
                cdt?.cancel()
                stopService(serviceIntent)
                isServiceOn = false
                mySharedPreference.setArmed(this, false)
                applyServiceState(false)
            } else {
                isServiceOn = true
                mySharedPreference.setArmed(this, true)
                applyServiceState(true)

                // Start service immediately while still in foreground (Android 12+ restriction)
                startForegroundService(serviceIntent)

                binding.countdownTimer.visibility = View.VISIBLE
                binding.image.setImageResource(R.drawable.stop2)
                binding.image.isEnabled = false

                cdt = object : CountDownTimer(10000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val seconds = millisUntilFinished / 1000
                        binding.countdownTimer.text =
                            "Activating in 00:%02d — don't move!".format(seconds)
                    }

                    override fun onFinish() {
                        cdt = null
                        binding.countdownTimer.visibility = View.GONE
                        binding.image.setImageResource(R.drawable.stop)
                        binding.image.isEnabled = true
                    }
                }.start()
            }
        }
    }

    private fun applyServiceState(running: Boolean) {
        if (running) {
            binding.soundPickerRow.visibility = View.INVISIBLE
            binding.sensitivitySection.visibility = View.INVISIBLE
            binding.btnHistory.visibility = View.INVISIBLE
            binding.image.setImageResource(R.drawable.stop)
            binding.statusBtn.text = "STOP"
        } else {
            binding.soundPickerRow.visibility = View.VISIBLE
            binding.sensitivitySection.visibility = View.VISIBLE
            binding.btnHistory.visibility = View.VISIBLE
            binding.image.setImageResource(R.drawable.turnonicon)
            binding.statusBtn.text = "START"
            binding.countdownTimer.visibility = View.GONE
            binding.image.isEnabled = true
        }
    }

    private fun updateSensitivityHint(index: Int) {
        binding.tvSensitivityHint.text = when (index) {
            0 -> "Needs a strong shake to trigger"
            1 -> "Responds to moderate movement"
            else -> "Triggers on the slightest touch"
        }
    }

    private fun updateSoundLabel() {
        val title = if (selectedPosition >= 0) {
            Constants.getlist()[selectedPosition].title.replace('_', ' ')
                .replaceFirstChar { it.uppercaseChar() }
        } else {
            "None selected"
        }
        binding.tvSelectedSound.text = title
    }

    override fun onResume() {
        super.onResume()
        val nowRunning = isServiceRunning(applicationContext, FourgroundService::class.java)
        if (nowRunning != isServiceOn && cdt == null) {
            isServiceOn = nowRunning
            applyServiceState(nowRunning)
        }
        maybeShowRatingPrompt()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, AlarmHistoryActivity::class.java))
                true
            }
            R.id.action_share -> {
                shareApp()
                true
            }
            R.id.action_help -> {
                startActivity(Intent(this, OnboardingActivity::class.java))
                true
            }
            R.id.action_privacy -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareApp() {
        val text = "Check out \"Don't Touch My Phone\" — it sets off a loud alarm if anyone picks up your phone without your PIN! " +
                "https://play.google.com/store/apps/details?id=${packageName}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share via"))
    }

    private fun maybeShowRatingPrompt() {
        if (mySharedPreference.hasRated(this)) return
        if (mySharedPreference.getTriggerCount(this) < 3) return
        try {
            val manager = ReviewManagerFactory.create(this)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(this, task.result).addOnCompleteListener {
                        mySharedPreference.setHasRated(this)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun showRationaleDialog(title: String, message: String, permission: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                notificationPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        cdt?.cancel()
        super.onDestroy()
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { serviceClass.name == it.service.className }
    }
}
