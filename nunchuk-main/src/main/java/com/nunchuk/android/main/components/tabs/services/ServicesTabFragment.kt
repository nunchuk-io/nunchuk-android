package com.nunchuk.android.main.components.tabs.services

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import com.nunchuk.android.main.nonsubscriber.NonSubscriberActivity
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCVerticalInputDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServicesTabFragment : BaseFragment<FragmentServicesTabBinding>() {

    private val viewModel: ServicesTabViewModel by viewModels()
    private lateinit var adapter: ServicesTabAdapter

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val isUpdate =
                    data.getBoolean(GlobalResultKey.UPDATE_INHERITANCE)
                if (isUpdate) viewModel.updateInheritance()
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentServicesTabBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        flowObserver(viewModel.event) { event ->
            when (event) {
                is ServicesTabEvent.GetServerKeySuccess -> openServerKeyDetail(event)
                is ServicesTabEvent.ProcessFailure -> showError(message = event.message)
                is ServicesTabEvent.Loading -> showOrHideLoading(loading = event.loading)
                is ServicesTabEvent.CheckPasswordSuccess -> handleCheckPasswordSuccess(event)
                is ServicesTabEvent.CreateSupportRoomSuccess -> navigator.openRoomDetailActivity(
                    requireContext(),
                    event.roomId
                )
                is ServicesTabEvent.LoadingEvent -> showOrHideLoading(event.isLoading)
                is ServicesTabEvent.CheckInheritance -> {
                    if (event.inheritanceCheck.isPaid) {
                        navigator.openInheritancePlanningScreen(
                            launcher = launcher,
                            requireContext(),
                            flowInfo = InheritancePlanFlow.CLAIM
                        )
                    } else {
                        showUnPaid()
                    }
                }
                is ServicesTabEvent.EmailInvalid -> showError(getString(R.string.nc_text_email_invalid))
                is ServicesTabEvent.OnSubmitEmailSuccess -> showSuccess(message = getString(R.string.nc_we_sent_an_email, event.email))
            }
        }
        flowObserver(viewModel.state) { state ->
            adapter.submitList(viewModel.getRowItems())
            state.isPremiumUser?.let {
                binding.supportFab.isVisible = state.isPremiumUser
                binding.actionGroup.isVisible = state.isPremiumUser.not()
                binding.claimLayout.isVisible = state.isPremiumUser.not()
            }
        }
    }

    private fun handleGoOurWebsite() {
        requireActivity().openExternalLink("https://nunchuk.io")
    }

    private fun showTellMeMoreDialog() {
        NCVerticalInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_email),
            positiveText = getString(R.string.nc_send_me_the_info),
            negativeText = getString(R.string.nc_visit_our_website),
            neutralText = getString(R.string.nc_text_do_this_later),
            defaultInput = viewModel.getEmail(),
            cancellable = true,
            onPositiveClicked = {
                viewModel.submitEmail(it)
            },
            onNegativeClicked = ::handleGoOurWebsite
        )
    }

    private fun handleCheckPasswordSuccess(event: ServicesTabEvent.CheckPasswordSuccess) {
        when (event.item) {
            ServiceTabRowItem.CoSigningPolicies -> {
                viewModel.getServiceKey(event.token)
            }
            ServiceTabRowItem.EmergencyLockdown -> {
                navigator.openEmergencyLockdownScreen(requireContext(), event.token)
            }
            ServiceTabRowItem.ViewInheritancePlan -> {
                navigator.openInheritancePlanningScreen(
                    launcher = launcher,
                    requireContext(),
                    verifyToken = event.token,
                    inheritance = viewModel.getInheritance(),
                    flowInfo = InheritancePlanFlow.VIEW
                )
            }
            else -> {}
        }
    }

    private fun setupViews() {
        adapter = ServicesTabAdapter(itemClick = {
            onTabItemClick(it)
        }, bannerClick = {
            NonSubscriberActivity.start(requireActivity(), it)
        })
        binding.recyclerView.adapter = adapter
        binding.supportFab.setOnDebounceClickListener {
            viewModel.getOrCreateSupportRom()
        }
        binding.claimLayout.setOnDebounceClickListener {
            if (viewModel.isLoggedIn()) {
                viewModel.checkInheritance()
            } else {
                navigator.openSignInScreen(requireActivity(), isNeedNewTask = false)
            }
        }
        binding.btnTellMore.setOnDebounceClickListener {
            showTellMeMoreDialog()
        }
        binding.btnVisitWebsite.setOnDebounceClickListener {
            handleGoOurWebsite()
        }
    }

    private fun onTabItemClick(item: ServiceTabRowItem) {
        if (isCheckWalletCreationState(item)) {
            val textAction =
                if (viewModel.getGroupStage() == MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS) {
                    getString(R.string.nc_continue_setting_up_wallet)
                } else if (viewModel.getGroupStage() == MembershipStage.NONE) {
                    getString(R.string.nc_start_wizard)
                } else {
                    ""
                }
            if (textAction.isNotBlank()) {
                showFeatureAssistedWalletInformDialog(textAction)
                return
            }
        }
        when (item) {
            ServiceTabRowItem.ClaimInheritance -> viewModel.checkInheritance()
            ServiceTabRowItem.CoSigningPolicies, ServiceTabRowItem.EmergencyLockdown -> enterPasswordDialog(item)
            ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(requireContext())
            ServiceTabRowItem.ManageSubscription -> showManageSubscriptionDialog()
            ServiceTabRowItem.OrderNewHardware -> showOrderNewHardwareDialog()
            ServiceTabRowItem.RollOverAssistedWallet -> {}
            ServiceTabRowItem.SetUpInheritancePlan -> {
                navigator.openInheritancePlanningScreen(
                    launcher = launcher,
                    requireContext(),
                    flowInfo = InheritancePlanFlow.SETUP
                )
            }
            ServiceTabRowItem.ViewInheritancePlan -> enterPasswordDialog(item)
        }
    }

    private fun isCheckWalletCreationState(item: ServiceTabRowItem): Boolean {
        return item is ServiceTabRowItem.CoSigningPolicies ||
                item is ServiceTabRowItem.EmergencyLockdown ||
                item is ServiceTabRowItem.RollOverAssistedWallet ||
                item is ServiceTabRowItem.KeyRecovery ||
                item is ServiceTabRowItem.SetUpInheritancePlan
    }

    private fun showUnPaid() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_unpaid_security_deposit),
            btnYes = getString(R.string.nc_take_me_there),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                val link = if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/claim" else "https://www.nunchuk.io/claim"
                requireActivity().openExternalLink(link)
            }
        )
    }

    private fun showFeatureAssistedWalletInformDialog(textAction: String) {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_feature_assisted_wallet_inform_desc),
            btnYes = textAction,
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                navigator.openMembershipActivity(
                    requireActivity(),
                    viewModel.getGroupStage()
                )
            }
        )
    }

    private fun enterPasswordDialog(item: ServiceTabRowItem) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                viewModel.confirmPassword(it, item)
            }
        )
    }

    private fun openServerKeyDetail(event: ServicesTabEvent.GetServerKeySuccess) {
        CosigningPolicyActivity.start(
            activity = requireActivity(),
            keyPolicy = null,
            xfp = event.signer.masterFingerprint,
            token = event.token,
            walletId = event.walletId,
        )
    }

    private fun showManageSubscriptionDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_manage_subscription_desc),
            onInfoClick = {
                val link = if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/my-plan" else "https://www.nunchuk.io/my-plan"
                requireActivity().openExternalLink(link)
            })
    }

    private fun showOrderNewHardwareDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_order_new_hardware_desc),
            onInfoClick = {
                val link = if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/hardware-replacement" else "https://www.nunchuk.io/hardware-replacement"
                requireActivity().openExternalLink(link)
            })
    }
}