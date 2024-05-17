package com.seventhelement.donttouchmyphone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.seventhelement.donttouchmyphone.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : AppCompatActivity() {
    private val splashTimeout: Long = 3000 // 3 seconds
    private val loaderDelay: Long = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        val binding= ActivitySplashScreenBinding.inflate(layoutInflater)


        // Handler to start main activity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, splashTimeout)
    }
}
