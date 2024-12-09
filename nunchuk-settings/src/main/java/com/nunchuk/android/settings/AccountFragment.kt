/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.account.SignInType
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.guestmode.isPrimaryKey
import com.nunchuk.android.core.media.NcMediaManager
import com.nunchuk.android.core.referral.ReferralArgs
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.loadImage
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.campaigns.CampaignType
import com.nunchuk.android.settings.AccountEvent.SignOutEvent
import com.nunchuk.android.settings.databinding.FragmentAccountBinding
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class AccountFragment : BaseCameraFragment<FragmentAccountBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    @Inject
    lateinit var ncMediaManager: NcMediaManager

    private val viewModel: AccountViewModel by activityViewModels()

    private var currentCaptureUri: Uri? = null

    private val selectPhotoAndVideoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            it ?: return@registerForActivityResult
            viewModel.uploadPhotoToMaTrix(it)
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            currentCaptureUri?.takeIf { isSuccess }?.let { uri ->
                viewModel.uploadPhotoToMaTrix(uri)
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAccountBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            currentCaptureUri = it.parcelable(KEY_CURRENT_PHOTO_PATH)
        }
        setupViews()
        setupData()
        observeEvent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_CURRENT_PHOTO_PATH, currentCaptureUri)
        super.onSaveInstanceState(outState)
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: AccountState) {
        val isGuestMode = signInModeHolder.getCurrentMode().isGuestMode()
        if (isGuestMode) {
            handleSetupGuestProfile()
        } else {
            binding.avatar.loadImage(
                imageUrl = state.account.avatarUrl.orEmpty().fromMxcUriToMatrixDownloadUrl(),
                circleCrop = true,
                cornerRadius = null,
                errorHolder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_account),
                placeHolder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_account)
            )
            binding.avatar.isInvisible = state.account.avatarUrl?.isEmpty().orFalse()

            binding.avatarHolder.text = state.account.name.shorten()
            binding.avatarHolder.isInvisible = state.account.avatarUrl?.isNotEmpty().orFalse()

            binding.name.text = state.account.name
            if (signInModeHolder.getCurrentMode().isPrimaryKey()) {
                binding.primaryTag.isVisible = true
                binding.subContent.isVisible =
                    state.account.primaryKeyInfo?.xfp.isNullOrBlank().not()
                binding.subContent.text =
                    String.format(
                        getString(R.string.nc_primary_key_account_xfp),
                        state.account.primaryKeyInfo?.xfp
                    )
                binding.name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                binding.name.isClickable = false
            } else {
                binding.subContent.text = state.account.email
            }
        }

        binding.layoutSync.root.isVisible = state.isSyncing()
        binding.layoutSync.tvSyncingPercent.text = "${state.syncProgress}%"
        binding.layoutSync.progressBarSyncing.progress = state.syncProgress

        binding.btnSignOut.isVisible = !isGuestMode
        binding.signIn.isVisible = isGuestMode
        binding.signUp.isVisible = isGuestMode
        binding.accountSettings.isVisible = !isGuestMode

        binding.localCurrency.text = getString(R.string.nc_local_currency_data, state.localCurrency)

        binding.premiumBadge.isVisible = state.plans.isNotEmpty()
        binding.premiumBadge.text = getPlanName(state.plans)
        binding.inviteFriendsView.isVisible = state.campaign?.isValid().orFalse() && (state.isHasWallet ||  state.campaign?.type == CampaignType.DOWNLOAD)
        binding.llCampaigns.isVisible = state.campaign?.cta.isNullOrEmpty().not() && state.campaign?.type != CampaignType.DOWNLOAD
        binding.tvCampaigns.text = state.campaign?.cta.orEmpty()
    }

    private fun openAboutScreen() {
        navigator.openAboutScreen(requireActivity())
    }

    private fun changeNetworkSetting() {
        navigator.openNetworkSettingScreen(requireActivity())
    }

    private fun openDisplaySettings() {
        navigator.openDisplaySettingsScreen(requireActivity())
    }

    private fun changeAvatar() {
        val bottomSheet = EditPhotoUserBottomSheet.show(
            fragmentManager = childFragmentManager,
            viewModel.getCurrentAccountInfo().avatarUrl.isNullOrEmpty().not()
        )
        bottomSheet.listener = {
            when (it) {
                EditPhotoOption.SelectAlbum -> {
                    openAlbum()
                }

                EditPhotoOption.TakePhoto -> {
                    requestCameraPermissionOrExecuteAction()
                }

                EditPhotoOption.RemovePhoto -> {
                    removePhoto()
                }
            }
        }
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        takePhoto()
    }

    private fun removePhoto() {
        viewModel.updateUserProfile(
            name = viewModel.getCurrentAccountInfo().name,
            avatarUrl = null
        )
    }

    private fun editName() {
        val bottomSheet = EditNameUserBottomSheet.show(
            name = viewModel.getCurrentAccountInfo().name,
            fragmentManager = childFragmentManager
        )

        bottomSheet.listener = {
            if (it is EditNameUserOption.Save) {
                val avatarUrl = viewModel.getCurrentAccountInfo().avatarUrl
                viewModel.updateUserProfile(
                    name = it.name,
                    avatarUrl = if (avatarUrl?.isNotEmpty().orFalse()) avatarUrl else null
                )
            }
        }
    }

    private fun handleEvent(event: AccountEvent) {
        when (event) {
            SignOutEvent -> {
                hideLoading()
                navigator.restartApp(requireActivity())
            }

            is AccountEvent.GetUserProfileSuccessEvent -> {
            }

            is AccountEvent.UploadPhotoSuccessEvent -> {
                viewModel.updateUserProfile(
                    name = viewModel.getCurrentAccountInfo().name,
                    avatarUrl = event.matrixUri
                )
            }

            is AccountEvent.LoadingEvent -> showOrHideLoading(event.loading)
            is AccountEvent.ShowError -> showError(event.message)
        }
    }

    private fun handleSetupGuestProfile() {
        binding.avatar.isInvisible = false
        binding.avatarHolder.isInvisible = true
        binding.avatar.loadImage(
            imageUrl = getString(R.string.nc_txt_guest),
            circleCrop = true,
            cornerRadius = null,
            errorHolder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_avatar),
            placeHolder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_avatar)
        )
        binding.takePicture.isVisible = false
        binding.name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        binding.name.setTextAppearance(R.style.NCText_Title)
        binding.subContent.setTextAppearance(R.style.NCText_Body)
        binding.name.text = getString(R.string.nc_do_more_with_nunchuk)
        binding.subContent.text = getString(R.string.nc_create_account_to_take_advantage)
    }

    private fun openAlbum() {
        selectPhotoAndVideoLauncher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                ).build()
        )
    }

    private fun takePhoto() {
        runCatching {
            ncMediaManager.createImageFile()
        }.getOrNull()?.let {
            val uri: Uri = FileProvider.getUriForFile(
                requireActivity(),
                "${requireActivity().packageName}.provider",
                it
            )
            currentCaptureUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    private fun setupViews() {
        binding.btnSignOut.setOnClickListener { viewModel.handleSignOutEvent() }
        binding.signIn.setOnClickListener {
            navigator.openSignInScreen(
                activityContext = requireActivity(),
                isNeedNewTask = false,
                type = SignInType.GUEST,
            )
        }
        binding.signUp.setOnClickListener {
            navigator.openSignUpScreen(requireActivity())
        }

        binding.displaySettings.setOnClickListener { openDisplaySettings() }
        binding.network.setOnClickListener { changeNetworkSetting() }
        binding.about.setOnClickListener { openAboutScreen() }
        binding.developerMode.setOnClickListener { openDeveloperScreen() }
        binding.walletSecuritySettings.setOnClickListener {
            navigator.openWalletSecuritySettingScreen(
                requireContext(),
                WalletSecurityArgs(type = WalletSecurityType.CREATE_PIN)
            )
        }
        binding.localCurrency.setOnClickListener { navigator.openLocalCurrencyScreen(requireContext()) }
        if (signInModeHolder.getCurrentMode().isGuestMode()) {
            binding.name.setOnClickListener(null)
            binding.takePicture.setOnClickListener(null)
            binding.accountSettings.setOnClickListener(null)
        } else {
            binding.name.setOnClickListener { editName() }
            binding.takePicture.setOnClickListener { changeAvatar() }
            binding.accountSettings.setOnClickListener {
                AccountSettingActivity.start(
                    requireActivity()
                )
            }
        }
        binding.inviteFriendsView.setOnClickListener {
            navigator.openReferralScreen(
                activityContext = requireActivity(),
                args = ReferralArgs(
                    campaign = viewModel.getCampaign()!!,
                    localReferrerCode = viewModel.getLocalReferrerCode()
                )
            )
        }
    }

    private fun openDeveloperScreen() {
        navigator.openDeveloperScreen(requireActivity())
    }

    private fun setupData() {
        viewModel.getCurrentUser()
    }

    private fun getPlanName(plans: List<MembershipPlan>): String {
        if (plans.isEmpty()) return ""
        if (plans.size > 1) return getString(R.string.nc_multiple_plans)
        return when(plans.first()) {
            MembershipPlan.HONEY_BADGER -> getString(R.string.nc_honey_badger)
            MembershipPlan.IRON_HAND -> getString(R.string.nc_iron_hand)
            MembershipPlan.BYZANTINE -> getString(R.string.nc_byzantine)
            MembershipPlan.BYZANTINE_PRO -> getString(R.string.nc_byzantine_pro)
            MembershipPlan.BYZANTINE_PREMIER -> getString(R.string.nc_byzantine_premier)
            MembershipPlan.FINNEY -> getString(R.string.nc_finney)
            MembershipPlan.FINNEY_PRO -> getString(R.string.nc_finney_pro)
            MembershipPlan.HONEY_BADGER_PLUS -> getString(R.string.nc_honey_badger_plus)
            MembershipPlan.HONEY_BADGER_PREMIER -> getString(R.string.nc_honey_badger_premier)
            else -> ""
        }
    }

    companion object {
        private const val KEY_CURRENT_PHOTO_PATH = "_a"
    }

}