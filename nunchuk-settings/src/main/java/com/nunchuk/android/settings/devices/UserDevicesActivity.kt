package com.nunchuk.android.settings.devices

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.profile.UserDeviceResponse
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.ActivityUserDevicesBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserDevicesActivity : BaseActivity<ActivityUserDevicesBinding>() {

    private val viewModel: UserDevicesViewModel by viewModels()

    private lateinit var adapter: UserDevicesAdapter

    override fun initializeBinding() = ActivityUserDevicesBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        setupDate()
        observeEvent()
    }

    private fun setupDate() {
        viewModel.getUserDevices()
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: UserDeviceEvent) {
        when (event) {
            is UserDeviceEvent.Loading -> if (event.loading) {
                showLoading(true)
            } else {
                hideLoading()
            }
            is UserDeviceEvent.DeleteDevicesSuccessEvent -> {
                hideLoading()
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_device_deleted_msg, event.device.name),
                    icon = R.drawable.ic_check_circle_outline
                )
                viewModel.getUserDevices()
            }
            is UserDeviceEvent.CompromisedDevicesSuccessEvent -> {
                hideLoading()
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_device_compromised_msg, event.device.name),
                    icon = R.drawable.ic_check_circle_outline
                )
                viewModel.getUserDevices()
            }
            UserDeviceEvent.GetDevicesErrorEvent -> {
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_error_get_devices),
                    icon = R.drawable.ic_check_circle_outline
                )
            }
            UserDeviceEvent.SignOutAllSuccessEvent -> {
                hideLoading()
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_signd_out_all_devices),
                    icon = R.drawable.ic_check_circle_outline
                )
                viewModel.getUserDevices()
            }
        }
    }

    private fun handleState(state: UserDeviceState) {
        adapter.items = state.devices
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.signOutAll.setOnClickListener { signOutAllDevices(viewModel.getCurrentDeviceList()) }
        adapter = UserDevicesAdapter { device ->
            val bottomSheet = ModifyUserDeviceBottomSheet.show(
                fragmentManager = supportFragmentManager,
                deviceName = device.name.orEmpty()
            )
            bottomSheet.listener = { option ->
                when (option) {
                    ModifyDeviceOption.MarkCompromise -> showConfirmDialogCompromise(device)
                    ModifyDeviceOption.SignOut -> showConfirmDialogSignOut(device)
                }
            }

        }
        binding.rcUserDevices.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.rcUserDevices.adapter = adapter

    }

    private fun showConfirmDialogSignOut(device: UserDeviceResponse) {

        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_txt_dialog_sign_out_message, device.name),
            onYesClick = { viewModel.deleteDevices(listOf(device)) }
        )

    }

    private fun showConfirmDialogCompromise(device: UserDeviceResponse) {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_txt_dialog_compromised_message, device.name),
            onYesClick = { viewModel.markCompromised(listOf(device)) }
        )
    }

    private fun signOutAllDevices(devices: List<UserDeviceResponse>) {
        if (devices.isEmpty()) {
            return
        }
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_txt_dialog_sign_out_all_message),
            onYesClick = { viewModel.deleteDevices(devices) }
        )
    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, UserDevicesActivity::class.java))
        }

    }
}