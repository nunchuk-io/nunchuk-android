package com.nunchuk.android.signer.ss.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityRecoverSeedBinding
import com.nunchuk.android.widget.util.heightExtended
import javax.inject.Inject

class RecoverSeedActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: RecoverSeedViewModel by lazy {
        ViewModelProviders.of(this, factory).get(RecoverSeedViewModel::class.java)
    }

    private lateinit var binding: ActivityRecoverSeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecoverSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()

    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: RecoverSeedState) {
    }

    private fun handleEvent(event: RecoverSeedEvent) {
    }

    private fun setupViews() {
        binding.phrase.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, RecoverSeedActivity::class.java))
        }
    }

}
