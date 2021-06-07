package com.nunchuk.android.qr

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.wallet.databinding.ActivityWalletDynamicQrBinding
import com.nunchuk.android.widget.util.setLightStatusBar

class DynamicQRCodeActivity : AppCompatActivity() {

    private val args: DynamicQRCodeArgs by lazy { DynamicQRCodeArgs.deserializeFrom(intent) }

    private lateinit var bitmaps: List<Bitmap>

    private lateinit var binding: ActivityWalletDynamicQrBinding

    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityWalletDynamicQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            handler.postDelayed(this, INTERVAL)
            bindQrCodes()
        }
    }

    private fun setupViews() {
        bitmaps = args.values.mapNotNull(String::convertToQRCode)
        binding.btnClose.setOnClickListener { finish() }
        handler.post(updateTextTask)

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun bindQrCodes() {
        calculateIndex()
        binding.qrCode.setImageBitmap(bitmaps[index])
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTextTask)
    }

    private fun calculateIndex() {
        index++
        if (index >= bitmaps.size) {
            index = 0
        }
    }

    companion object {
        const val INTERVAL = 200L

        private var handler = Handler(Looper.getMainLooper())

        fun start(activityContext: Context, values: List<String>) {
            activityContext.startActivity(DynamicQRCodeArgs(values).buildIntent(activityContext))
        }
    }

}

