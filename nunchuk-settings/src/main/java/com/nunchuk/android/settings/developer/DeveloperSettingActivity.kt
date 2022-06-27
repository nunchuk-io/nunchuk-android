package com.nunchuk.android.settings.developer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.domain.data.DeveloperSetting
import com.nunchuk.android.settings.databinding.ActivityDeveloperSettingBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeveloperSettingActivity : BaseActivity<ActivityDeveloperSettingBinding>() {

    private val viewModel: DeveloperSettingViewModel by viewModels()

    override fun initializeBinding() = ActivityDeveloperSettingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        showToolbarBackButton()

        setupViews()
        setupData()
        observeEvent()
    }

    private fun showToolbarBackButton() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: DeveloperSettingState) {
        binding.switchDebugMode.isChecked = state.developerSetting.debugMode
    }

    private fun handleEvent(event: DeveloperSettingEvent) {
        when (event) {
            is DeveloperSettingEvent.UpdateSuccessEvent -> {
                // currently we do nothing
            }
        }
    }

    private fun setupViews() {
        binding.switchDebugMode.setOnCheckedChangeListener { _, checked ->
            updateDisplayUnitSetting(
                debugMode = checked
            )
        }
    }

    private fun updateDisplayUnitSetting(
        debugMode: Boolean
    ) {
        viewModel.updateDeveloperSettings(
            developerSetting = DeveloperSetting(
                debugMode = debugMode
            )
        )
    }

    private fun setupData() {
        viewModel.getDeveloperSettings()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, DeveloperSettingActivity::class.java))
        }
    }
}