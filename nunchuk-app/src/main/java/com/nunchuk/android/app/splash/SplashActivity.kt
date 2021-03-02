package com.nunchuk.android.app.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.databinding.ActivitySplashBinding
import io.reactivex.Completable

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.greeting.text = "Hello!!!"
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        Completable.fromCallable { Log.d(TAG, "add execute function later") }
                .defaultSchedulers()
                .subscribe({
                    Log.d(TAG, "retrieve data completed")
                }, {
                    Log.e(TAG, "retrieve data error", it)
                })
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}

