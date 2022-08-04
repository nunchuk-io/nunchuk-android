package com.nunchuk.android.wallet.personal.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.wallet.personal.databinding.FragmentWalletIntermediaryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletIntermediaryFragment : BaseFragment<FragmentWalletIntermediaryBinding>() {
    private val viewModel: WalletIntermediaryViewModel by viewModels()
    private val args: WalletIntermediaryFragmentArgs by navArgs()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletIntermediaryBinding {
        return FragmentWalletIntermediaryBinding.inflate(inflater, container, false)
    }

    private val hasSigner
        get() = requireArguments().getBoolean(WalletIntermediaryActivity.EXTRA_HAS_SIGNER, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        setupViews()
        observer()
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            showOrHideNfcLoading(it is WalletIntermediaryEvent.Loading)
            when (it) {
                is WalletIntermediaryEvent.OnLoadFileSuccess -> handleLoadFilePath(it)
            }
        }
    }

    private fun handleLoadFilePath(it: WalletIntermediaryEvent.OnLoadFileSuccess) {
        if (it.path.isNotEmpty()) {
            navigator.openAddRecoverWalletScreen(
                requireActivity(), RecoverWalletData(
                    type = RecoverWalletType.FILE,
                    filePath = it.path
                )
            )
        }
    }

    private fun initUi() {
        if (args.isQuickWallet) {
            binding.title.isVisible = true
            binding.message.text = getString(R.string.nc_create_single_sig_for_sweep)
            binding.btnCreateNewWallet.text = getString(R.string.nc_text_continue)
            binding.btnRecoverWallet.text = getString(R.string.nc_create_my_own_wallet)
        }
    }

    private fun openCreateNewWalletScreen() {
        navigator.openAddWalletScreen(requireContext())
    }

    private fun openRecoverWalletScreen() {
        val recoverWalletBottomSheet = RecoverWalletActionBottomSheet.show(childFragmentManager)
        recoverWalletBottomSheet.listener = {
            when (it) {
                RecoverWalletOption.QrCode -> handleOptionUsingQRCode()
                RecoverWalletOption.BSMSFile -> openSelectFileChooser(WalletIntermediaryActivity.REQUEST_CODE)
            }
        }
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == WalletIntermediaryActivity.REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            intent?.data?.let {
                viewModel.extractFilePath(it)
            }
        }
    }

    private fun setupViews() {
        binding.btnCreateNewWallet.setOnClickListener {
            if (args.isQuickWallet) {
                navigator.openCreateNewSeedScreen(this, true)
            } else if (hasSigner) {
                openCreateNewWalletScreen()
            } else {
                openWalletEmptySignerScreen()
            }
        }
        binding.btnRecoverWallet.setOnClickListener {
            if (args.isQuickWallet) {
                navigator.openWalletIntermediaryScreen(requireActivity(), viewModel.hasSigner)
                requireActivity().finish()
            } else {
                openRecoverWalletScreen()
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun openWalletEmptySignerScreen() {
        navigator.openWalletEmptySignerScreen(requireActivity())
    }

    private fun handleOptionUsingQRCode() {
        if (requireActivity().isPermissionGranted(Manifest.permission.CAMERA)) {
            openScanQRCodeScreen()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), WalletIntermediaryActivity.REQUEST_PERMISSION_CAMERA)
        }
    }


    // TODO: refactor with registerForActivityResult later
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WalletIntermediaryActivity.REQUEST_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePermissionGranted()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showAlertPermissionNotGranted()
            } else {
                showAlertPermissionDeniedPermanently()
            }
        }
    }

    private fun handlePermissionGranted() {
        openScanQRCodeScreen()
    }

    private fun showAlertPermissionNotGranted() {
        requireActivity().showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                handleOptionUsingQRCode()
            },
            negativeClick = {
            }
        )
    }

    private fun showAlertPermissionDeniedPermanently() {
        requireActivity().showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied_permanently),
            message = getString(R.string.nc_text_des_permission_denied_permanently),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                requireActivity().startActivityAppSetting()
            },
            negativeClick = {
            }
        )
    }


}