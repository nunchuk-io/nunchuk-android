package com.nunchuk.android.app.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.nativelib.LibNunchukFacade

class SplashActivity : AppCompatActivity() {

    // DI
    private val nunchuk = LibNunchukFacade()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.greeting.text = "Hello!!!"
        nunchuk.retrieveData()
    }
}

