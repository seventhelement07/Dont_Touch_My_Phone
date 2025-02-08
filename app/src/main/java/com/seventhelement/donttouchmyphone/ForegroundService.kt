package com.seventhelement.donttouchmyphone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
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
import android.widget.Toast
import androidx.annotation.RequiresApi


private var sensorMan: SensorManager? = null
private var mSensorManager: SensorManager? = null
private var mSensor: Sensor? = null
private var accelerometer: Sensor? = null
private val mGravity: FloatArray?=null
private var mAccel = 0f
private var mAccelCurrent = 0f
private  var mAccelLast = 0f
private var cameraManager: CameraManager? = null
private var cameraId: String? = null

private val handler = Handler()
private var isFlashOn = false
private const val isRecording = false
private var mediaPlayer: MediaPlayer? = null
var song = ""
class FourgroundService: Service(), SensorEventListener {
    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    override fun onCreate() {
        super.onCreate()


    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        song = intent!!.getStringExtra("message_key2")!!
        var resourceId: Int =getResources().getIdentifier(song, "raw", packageName)
        mediaPlayer = MediaPlayer.create(this, resourceId)
        Toast.makeText(this,"gaming",Toast.LENGTH_SHORT).show()
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager!!.getCameraIdList()[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }


//        TextView textView = (TextView) findViewById(R.id.textViewName);
//        textView.setText("text you want to display");
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensorMan = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorMan!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorMan!!.registerListener(
            this, accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        mSensorManager!!.registerListener(
            this, mSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        //alertDialog.show();
        mAccel = 0.00f
        mAccelCurrent = SensorManager.GRAVITY_EARTH
        mAccelLast = SensorManager.GRAVITY_EARTH
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0)
        val CHANNEL_ID = "Foreground Service"
        var channel: NotificationChannel? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                channel!!
            )
        }
        val intent1 = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_IMMUTABLE)
        var notification: Notification.Builder? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = Notification.Builder(applicationContext, CHANNEL_ID).setContentTitle("Don't Touch My Phone")
                .setContentText("Started").setContentIntent(pendingIntent)
        }


        startForeground(1001, notification!!.build())
        return START_NOT_STICKY
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val mGravity = event!!.values.clone()
            // Shake detection
            // Shake detection
            val x: Float = mGravity.get(0)
            val y: Float = mGravity.get(1)
            val z: Float = mGravity.get(2)
            val mAccelLast = mAccelCurrent
            mAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = mAccelCurrent - mAccelLast
            mAccel = mAccel * 0.9f + delta
            // Make this higher or lower according to how much
            // motion you want to detect
            if (mAccel > 0.5) {


                mediaPlayer!!.isLooping = true
                turnOnFlashlight()
                isFlashOn = true
                mediaPlayer!!.start()
//                Toast.makeText(this,"Motion",Toast.LENGTH_SHORT).show()
                return
            }

            }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
    override fun onDestroy() {

        // Set the flag to false to stop the loop


        //stopFlashingAndRinging();
        turnOffFlashlight()
        if (mediaPlayer != null) {
            mediaPlayer!!.stop();
            mediaPlayer!!.release();
            mediaPlayer = null;
        }
        mSensorManager?.unregisterListener(this)
        super.onDestroy()
    }
    private fun turnOnFlashlight() {
        try {
            cameraId?.let { cameraManager?.setTorchMode(it, true) }
            isFlashOn = true
            // Start blinking after 500ms
            handler.postDelayed(blinkRunnable, 500)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun turnOffFlashlight() {
        try {
            cameraId?.let { cameraManager?.setTorchMode(it, false) }
            isFlashOn = false
            // Remove callbacks to stop blinking
            handler.removeCallbacks(blinkRunnable)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    private val blinkRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isFlashOn) {
                turnOffFlashlight()
            } else {
                turnOnFlashlight()
            }
            // Schedule next blink after 500ms
            handler.postDelayed(this, 500)
        }
    }
}