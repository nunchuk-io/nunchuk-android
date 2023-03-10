package com.nunchuk.android.wallet.components.coin

import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding

class CoinActivity : BaseActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}