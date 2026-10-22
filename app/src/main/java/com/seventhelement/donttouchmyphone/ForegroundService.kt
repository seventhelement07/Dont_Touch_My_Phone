package com.seventhelement.donttouchmyphone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.seventhelement.donttouchmyphone.db.AlarmDatabase
import com.seventhelement.donttouchmyphone.db.AlarmEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FourgroundService : android.app.Service(), SensorEventListener {

    companion object {
        const val ACTION_DISARM = "com.seventhelement.donttouchmyphone.DISARM"
        const val GRACE_MS = 5_000L
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var mAccel = 0f
    private var mAccelCurrent = SensorManager.GRAVITY_EARTH
    private var mAccelLast = SensorManager.GRAVITY_EARTH
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isFlashOn = false
    private var isServiceActive = false
    private var isAlarmActive = false
    private var isNearObject = false
    private var mediaPlayer: MediaPlayer? = null
    private var song = ""
    private var threshold = 0.8f
    private var sensorWakeLock: PowerManager.WakeLock? = null
    private var graceRunnable: Runnable? = null

    private val channelId = "Foreground Service"
    private val notifId = 1001
    private val mySharedPreference = MySharedPreference()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Tracks rapid volume-down presses for the silent-disarm knock (3× in 2s)
    private val volumeTimes = ArrayDeque<Long>()
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val newVol = intent?.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1) ?: return
            val prevVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", newVol)
            if (newVol >= prevVol) return  // Only count presses that lower the volume
            val now = System.currentTimeMillis()
            volumeTimes.addLast(now)
            while (volumeTimes.isNotEmpty() && now - volumeTimes.first() > 2000) volumeTimes.removeFirst()
            if (volumeTimes.size >= 3) {
                volumeTimes.clear()
                disarmSilently()
            }
        }
    }

    private val chargerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED && !isAlarmActive) {
                isAlarmActive = true
                triggerAlarm("Charger")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Guard against an update installing while this service was already armed from a
        // pre-salt build: if PinEntryActivity fires before MainActivity reopens, the stale
        // unsalted hash would never match again. Clear it now so the PIN flow stays consistent.
        mySharedPreference.migrateLegacyPinIfNeeded(this)

        song = intent?.getStringExtra("message_key2") ?: ""
        val sensitivityIndex = mySharedPreference.getSensitivityIndex(this)
        threshold = Constants.SENSITIVITY_THRESHOLDS[sensitivityIndex]

        // Prefer the resource ID passed directly; fall back to name lookup; then default to alarm
        val resourceId = intent?.getIntExtra("sound_res_id", 0)
            ?.takeIf { it != 0 }
            ?: run { resources.getIdentifier(song, "raw", packageName).takeIf { it != 0 } }
            ?: R.raw.alarm
        // USAGE_ALARM (not plain music) is what this actually is, and it's the attribute
        // Android 17's background-audio hardening specifically exempts from the new
        // foreground/while-in-use restrictions on background audio playback.
        val alarmAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        mediaPlayer = MediaPlayer.create(this, resourceId, alarmAttributes, 0)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager!!.cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)

        registerReceiver(chargerReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
        registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        isServiceActive = true
        startForeground(notifId, buildNotification("Place your phone down — activating in 10s…"))

        // Keep CPU alive so sensors fire when screen is off
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        sensorWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DontTouch::SensorWakeLock"
        )
        sensorWakeLock?.acquire()

        // Delay sensor registration so the user can set the phone down first
        handler.postDelayed({
            if (isServiceActive) {
                registerSensors()
                getSystemService(NotificationManager::class.java)
                    ?.notify(notifId, buildNotification("Motion detection active — don't touch!"))
            }
        }, 10_000L)

        return START_NOT_STICKY
    }

    private fun registerSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        proximitySensor?.let {
            sensorManager!!.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mAccel = 0f
        mAccelCurrent = SensorManager.GRAVITY_EARTH
        mAccelLast = SensorManager.GRAVITY_EARTH
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Don't Touch My Phone")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                isNearObject = event.values[0] < 1.0f
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                mAccelLast = mAccelCurrent
                mAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = mAccelCurrent - mAccelLast
                mAccel = mAccel * 0.9f + delta

                if (mAccel > threshold && !isNearObject && !isAlarmActive) {
                    isAlarmActive = true
                    triggerAlarm("Motion")
                }
            }
        }
    }

    private fun triggerAlarm(triggerType: String) {
        // Wake the screen so PIN entry is visible immediately
        @Suppress("DEPRECATION")
        (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "DontTouch::AlarmWakeup"
            ).acquire(10_000L)

        // Show PIN screen immediately — still silent during grace period
        startActivity(Intent(this, PinEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("grace_ms", GRACE_MS)
        })

        // Log the alarm event
        mySharedPreference.incrementTriggerCount(this)
        serviceScope.launch {
            AlarmDatabase.getInstance(this@FourgroundService)
                .alarmHistoryDao()
                .insertEvent(AlarmEvent(
                    timestamp = System.currentTimeMillis(),
                    triggerType = triggerType
                ))
        }

        // Start audio and flash after grace period — owner can enter PIN before alarm blares
        graceRunnable = Runnable {
            graceRunnable = null
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
            if (!isFlashOn) turnOnFlashlight()
        }.also { handler.postDelayed(it, GRACE_MS) }
    }

    private fun disarmSilently() {
        graceRunnable?.let { handler.removeCallbacks(it) }
        graceRunnable = null
        // Notify PinEntryActivity (if open) to close itself
        sendBroadcast(Intent(ACTION_DISARM).setPackage(packageName))
        stopSelf()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        isServiceActive = false
        isAlarmActive = false
        handler.removeCallbacksAndMessages(null)
        turnOffFlashlight()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        sensorManager?.unregisterListener(this)
        try { unregisterReceiver(chargerReceiver) } catch (_: IllegalArgumentException) {}
        try { unregisterReceiver(volumeReceiver) } catch (_: IllegalArgumentException) {}
        graceRunnable?.let { handler.removeCallbacks(it) }
        graceRunnable = null
        sensorWakeLock?.let { if (it.isHeld) it.release() }
        sensorWakeLock = null
        super.onDestroy()
    }

    private fun turnOnFlashlight() {
        try {
            isFlashOn = true
            cameraId?.let { cameraManager?.setTorchMode(it, true) }
            handler.postDelayed(blinkRunnable, 500)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun turnOffFlashlight() {
        handler.removeCallbacks(blinkRunnable)
        try {
            isFlashOn = false
            cameraId?.let { cameraManager?.setTorchMode(it, false) }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val blinkRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isServiceActive) return
            try {
                isFlashOn = !isFlashOn
                cameraId?.let { cameraManager?.setTorchMode(it, isFlashOn) }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
            handler.postDelayed(this, 500)
        }
    }
}
