package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import javax.inject.Inject

class SignerIntroActivity : BaseActivity() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private lateinit var binding: ActivitySignerIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignerIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.gotIt.setOnClickListener { openAddSignerScreen() }
    }

    private fun openAddSignerScreen() {
        finish()
        navigator.openAddSignerScreen(this)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java))
        }
    }

}