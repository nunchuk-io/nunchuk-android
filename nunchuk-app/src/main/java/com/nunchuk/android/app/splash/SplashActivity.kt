package com.nunchuk.android.app.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.usecase.createCreateSignerUseCase
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SplashActivity : AppCompatActivity() {

    private val useCase = createCreateSignerUseCase()
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.greeting.text = "Hello!!!"
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()

        Single.fromCallable {
            useCase.execute("TESTER",
                    "xpub6Gs9Gp1P7ov2Xy6XmVBawLUwRgifGMK93K6bYuMdi9PfmJ6y6e7ffzD7JKCjWgJn71YGCQMozL1284Ywoaptv8UGRsua635k8yELEKk9nhh",
                    "0297da76f2b4ae426f41e617b4f13243716d1417d3acc3f8da7a54f301fc951741",
                    "m/48'/0'/0'/7",
                    "0b93c52e"
            )

        }
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

