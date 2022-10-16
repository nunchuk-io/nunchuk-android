package com.nunchuk.android.signer.software.components.primarykey.intro.replace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeyReplaceKeyIntroBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeyReplaceKeyIntroActivity : BaseActivity<ActivityPkeyReplaceKeyIntroBinding>() {

    @Inject
    lateinit var primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder

    private val viewModel: PKeyReplaceKeyIntroViewModel by viewModels()

    override fun initializeBinding() = ActivityPkeyReplaceKeyIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: PKeyReplaceKeyIntroEvent) {
        when (event) {
            is PKeyReplaceKeyIntroEvent.LoadingEvent -> showOrHideLoading(loading = event.loading)
            is PKeyReplaceKeyIntroEvent.CheckNeedPassphraseSent -> showEnterPassphraseDialog(event.isNeeded)
        }
    }

    private fun setupViews() {
        binding.continueBtn.setOnDebounceClickListener {
            viewModel.checkNeedPassphraseSent()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun showEnterPassphraseDialog(isNeeded: Boolean) {
        if (isNeeded) {
            NCInputDialog(this).showDialog(
                title = getString(R.string.nc_transaction_enter_passphrase),
                onConfirmed = {
                    openNextScreen(it)
                }
            )
        } else {
            openNextScreen("")
        }
    }

    private fun openNextScreen(passphrase: String) {
        navigator.openAddPrimaryKeyScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = PrimaryKeyFlow.REPLACE
        )
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    PKeyReplaceKeyIntroActivity::class.java
                )
            )
        }
    }
}