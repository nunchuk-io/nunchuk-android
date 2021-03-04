package com.nunchuk.android.app.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.databinding.ActivitySplashBinding
import com.nunchuk.android.usecase.CreateSignerUseCase
import javax.inject.Inject

internal class SplashActivity : BaseActivity() {

    @Inject
    lateinit var useCase: CreateSignerUseCase

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.greeting.text = "Hello!!!"
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()

        useCase.execute(
                "TESTER",
                "xpub6Gs9Gp1P7ov2Xy6XmVBawLUwRgifGMK93K6bYuMdi9PfmJ6y6e7ffzD7JKCjWgJn71YGCQMozL1284Ywoaptv8UGRsua635k8yELEKk9nhh",
                "0297da76f2b4ae426f41e617b4f13243716d1417d3acc3f8da7a54f301fc951741",
                "m/48'/0'/0'/7",
                "0b93c52e"
        ).defaultSchedulers().subscribe({
            Log.d(TAG, "create signer success")
        }, {
            Log.e(TAG, "create signer error", it)
        })
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}

