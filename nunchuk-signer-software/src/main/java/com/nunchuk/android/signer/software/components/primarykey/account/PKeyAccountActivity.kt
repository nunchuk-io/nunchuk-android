package com.nunchuk.android.signer.software.components.primarykey.account

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.signer.software.databinding.ActivityPkeyAccountBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PKeyAccountActivity : BaseActivity<ActivityPkeyAccountBinding>() {

    private val viewModel: PKeyAccountViewModel by viewModels()

    private lateinit var adapter: PrimaryKeyAccountAdapter

    override fun initializeBinding() = ActivityPkeyAccountBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: PKeyAccountState) {
        adapter.submitList(state.primaryKeys)
    }

    private fun handleEvent(event: PKeyAccountEvent) {
        when (event) {
            is PKeyAccountEvent.LoadingEvent -> showOrHideLoading(loading = event.loading)
            is PKeyAccountEvent.ProcessErrorEvent -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun setupViews() {
        adapter = PrimaryKeyAccountAdapter {
            navigator.openPrimaryKeySignInScreen(this, it)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(
            activityContext: Context,
            accounts: ArrayList<PrimaryKey>
        ) {
            activityContext.startActivity(
                PKeyAccountArgs(
                    accounts = accounts,
                ).buildIntent(
                    activityContext
                )
            )
        }
    }
}