package com.seventhelement.donttouchmyphone

import Adapter
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.seventhelement.data1
import com.seventhelement.donttouchmyphone.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(),Adapter.OnItemSelectedListener {
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 123
        val description = "Test Notification"
    }
    lateinit var adapter:Adapter
    lateinit var binding: ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.O)
    var alertDialog: AlertDialog? = null
    var isServiceOn = false
    var cdt: CountDownTimer? = null
    val mySharedPreference=MySharedPreference();
    var seelctposition = -1
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {

            seelctposition = mySharedPreference.getInt(applicationContext);
            if (seelctposition == -1) {
                seelctposition = 0;
            }
        }
        catch(e:Exception)
        {

        }

        //Toast.makeText(this,seelctposition.toString(),Toast.LENGTH_SHORT).show();
        alertDialog = AlertDialog.Builder(this).create()
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.title = "Don't Touch My Phone"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermissionGranted = true
        }
        val list= ArrayList<data1>();
            list.add(data1("alarm",R.drawable.alarm));
            list.add(data1("applause", R.drawable.applause));
            list.add(data1("telephone", R.drawable.call));
            list.add(data1("cartoon", R.drawable.cartoon));
            list.add(data1("dog", R.drawable.dog));
            list.add(data1("gaming", R.drawable.gaming));
            list.add(data1("bike", R.drawable.bike));
            list.add(data1("buzzer", R.drawable.buzzer));
            list.add(data1("car", R.drawable.car));
            list.add(data1("iphone_alarm", R.drawable.iphone));
            list.add(data1("machine_gun", R.drawable.machin_gun));
            list.add(data1("meow", R.drawable.cat));
            list.add(data1("sniper", R.drawable.sniper));
         isServiceOn =isServiceRunning(applicationContext, FourgroundService::class.java)
        val intent=Intent(applicationContext,FourgroundService::class.java)
         adapter = Adapter(list, this, packageName, this, seelctposition!!,true)
        binding.recyclerView.layoutManager= GridLayoutManager(this,2);
        binding.recyclerView.adapter= adapter;


        if (isServiceOn) {
           binding.recyclerView.setVisibility(View.INVISIBLE)
            binding.select .setVisibility(View.INVISIBLE)
            binding.image.setImageResource(R.drawable.stop)
            binding.statusBtn.setText("STOP")
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE)
            binding.select.setVisibility(View.VISIBLE)
            binding.image.setImageResource(R.drawable.turnonicon)
            binding.statusBtn.setText("START")
        }

        binding.image.setOnClickListener {
            if (seelctposition == -1)
                Toast.makeText(this, "Please Select Any Sound To Continue", Toast.LENGTH_SHORT)
                    .show()
            else {
                intent.putExtra("message_key2", Constants.getlist()[seelctposition].title)
                if (isServiceOn) {
                    binding.recyclerView.setVisibility(View.VISIBLE)
                    binding.select.setVisibility(View.VISIBLE)
                    binding.statusBtn.text = "START SERVICE"
                    binding.image.setImageResource(R.drawable.turnonicon)
                    stopService(intent)
//                    isServiceOn=false;
//                    audioClassificationHelper.startAudioClassification();
//                    audioClassificationHelper.stopAudioClassification();
//                    isServiceOn=false;
//                    // MySharedPreferences.saveBoolean(getApplicationContext(), isServiceOn);
//                    stopFlashingAndRinging();
//                    turnOffFlashlight();
//                    mediaPlayer.pause();
//                    mediaPlayer.seekTo(0);
                    isServiceOn = false
                } else {
                    binding.image.setImageResource(R.drawable.stop)
                    //audioClassificationHelper.startAudioClassification();
                    isServiceOn = true
                    adapter.releaseMediaPlayer()
                    binding.statusBtn.text = "STOP SERVICE"
                    binding.recyclerView.setVisibility(View.INVISIBLE)
                    binding.select .setVisibility(View.INVISIBLE)
                    alertDialog!!.setTitle("Will Be Activated In 10 Seconds")
                    alertDialog!!.setMessage("00:10")
                    //Toast.makeText(MainActivity.this, "Motion Switch On", Toast.LENGTH_SHORT).show();
                    cdt = object : CountDownTimer(10000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            alertDialog!!.setMessage("00:" + millisUntilFinished / 1000)
                        }

                        override fun onFinish() {
                            //info.setVisibility(View.GONE);
                            //mSwitchSet = 1
                            alertDialog!!.hide()
                            Toast.makeText(
                                this@MainActivity,
                                "Motion Detection Mode Activated",
                                Toast.LENGTH_SHORT
                            ).show()
                            startForegroundService(intent)
                        }
                    }.start()
                    alertDialog!!.show()
                    alertDialog!!.setCancelable(false)


                }
            }
        }

    }
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (manager != null) {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasNotificationPermissionGranted = isGranted
            if (!isGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                            showNotificationPermissionRationale()
                        } else {
                            showSettingDialog()
                        }
                    }
                }
            } else {
                Toast.makeText(applicationContext, "notification permission granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun showSettingDialog() {
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setTitle("Notification Permission")
            .setMessage("Notification permission is required, Please allow notification permission from setting")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationPermissionRationale() {

        MaterialAlertDialogBuilder(this)
            .setTitle("Alert")
            .setMessage("Notification permission is required, to show notification")
            .setPositiveButton("Ok") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    var hasNotificationPermissionGranted = false
    override fun onItemSelected(position: Int) {
        seelctposition=position;
        mySharedPreference.saveInt(this,position)
    }

    override fun onItemDeselected() {
        TODO("Not yet implemented")
    }
}


