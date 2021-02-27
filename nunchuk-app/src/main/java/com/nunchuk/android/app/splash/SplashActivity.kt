package com.nunchuk.android.app.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.nativelib.LibNunchukFacade
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SplashActivity : AppCompatActivity() {

    // DI
    private val nunchuk = LibNunchukFacade()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.greeting.text = "Hello!!!"
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        Completable.fromCallable(nunchuk::retrieveData)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(TAG, "completed")
                }, {
                    Log.d(TAG, "completed")
                })
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}

