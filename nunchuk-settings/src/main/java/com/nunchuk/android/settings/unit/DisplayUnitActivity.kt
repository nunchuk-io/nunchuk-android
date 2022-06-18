package com.nunchuk.android.settings.unit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.entities.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.entities.DisplayUnitSetting
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.settings.databinding.ActivityDisplayUnitSettingBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayUnitActivity : BaseActivity<ActivityDisplayUnitSettingBinding>() {

    private val viewModel: DisplayUnitViewModel by viewModels()

    override fun initializeBinding() = ActivityDisplayUnitSettingBinding.inflate(layoutInflater)

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

    private fun handleState(state: DisplayUnitState) {
        binding.cbPrecision.isChecked = state.displayUnitSetting.showBTCPrecision
        binding.rbSAT.isChecked = state.displayUnitSetting.useSAT
        binding.rbBTC.isChecked = state.displayUnitSetting.useBTC
    }

    private fun handleEvent(event: DisplayUnitEvent) {
        when (event) {
            is DisplayUnitEvent.UpdateDisplayUnitSettingSuccessEvent -> {
                CURRENT_DISPLAY_UNIT_TYPE = event.displayUnitSetting.getCurrentDisplayUnitType()
            }
        }
    }

    private fun setupViews() {
        binding.rbSAT.setOnCheckedChangeListener { _, checked ->
            updateDisplayUnitSetting(
                useBTC = !checked,
                showBTCPrecision = !checked,
                useSAT = checked
            )
        }
        binding.rbBTC.setOnCheckedChangeListener { _, checked ->
            updateDisplayUnitSetting(
                useBTC = checked,
                showBTCPrecision = viewModel.currentDisplayUnitSettings?.showBTCPrecision.orFalse(),
                useSAT = !checked
            )
        }
        binding.cbPrecision.setOnCheckedChangeListener { _, checked ->
            updateDisplayUnitSetting(
                useBTC = viewModel.currentDisplayUnitSettings?.useBTC.orFalse(),
                showBTCPrecision = checked,
                useSAT = viewModel.currentDisplayUnitSettings?.useSAT.orFalse()
            )
        }
    }

    private fun updateDisplayUnitSetting(
        useBTC: Boolean,
        showBTCPrecision: Boolean,
        useSAT: Boolean
    ) {
        val currentDisplayUnitSetting = viewModel.currentDisplayUnitSettings
        val newDisplayUnitSetting = DisplayUnitSetting(
            useBTC = useBTC,
            showBTCPrecision = showBTCPrecision,
            useSAT = useSAT
        )

        if (currentDisplayUnitSetting != newDisplayUnitSetting) {
            viewModel.updateDisplayUnitSetting(
                newDisplayUnitSetting
            )
        }
    }

    private fun setupData() {
        viewModel.getDisplayUnitSetting()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, DisplayUnitActivity::class.java))
        }
    }
}