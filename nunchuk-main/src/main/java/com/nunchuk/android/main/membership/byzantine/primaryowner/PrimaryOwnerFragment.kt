package com.nunchuk.android.main.membership.byzantine.primaryowner

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.AddKeyStepViewModel
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.nav.args.BackUpWalletArgs
import com.nunchuk.android.nav.args.BackUpWalletType
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PrimaryOwnerFragment : MembershipFragment() {

    private val viewModel: PrimaryOwnerViewModel by viewModels()
    private val args: PrimaryOwnerFragmentArgs by navArgs()
    private val addKeyStepViewModel: AddKeyStepViewModel by activityViewModels()
    private val groupId: String by lazy { args.groupId }
    private val quickWalletParam by lazy { (activity as com.nunchuk.android.main.membership.MembershipActivity).quickWalletParam }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val replacedWalletId = (activity as? com.nunchuk.android.main.membership.MembershipActivity)?.onChainReplaceWalletId.orEmpty()
            findNavController().navigate(
                PrimaryOwnerFragmentDirections.actionPrimaryOwnerFragmentToCreateWalletSuccessFragment(
                    walletId = viewModel.walletId,
                    replacedWalletId = replacedWalletId
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                PrimaryOwnerScreen(
                    flow = args.flow, viewModel = viewModel,
                    membershipStepManager = membershipStepManager,
                    onSkipClick = {
                        if (args.flow == PrimaryOwnerFlow.EDIT) {
                            requireActivity().finish()
                        } else {
                            viewModel.createGroupWallet(args.groupId)
                        }
                    }, onMoreClicked = ::handleShowMore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is PrimaryOwnerEvent.Loading -> showOrHideLoading(event.isLoading)
                is PrimaryOwnerEvent.Error -> showError(event.message)
                PrimaryOwnerEvent.UpdatePrimaryOwnerSuccess -> {
                    NcToastManager.scheduleShowMessage(getString(R.string.nc_primary_owner_update_success))
                    requireActivity().finish()
                }

                is PrimaryOwnerEvent.OnCreateWalletSuccess -> {
                    addKeyStepViewModel.requireInheritance(event.wallet.id)
                    handleCreateWalletSuccess(event)
                }
                is PrimaryOwnerEvent.OpenUploadConfigurationScreen -> {
                    navigator.openUploadConfigurationScreen(
                        activityContext = requireActivity(),
                        walletId = event.walletId,
                        isOnChainFlow = true,
                        groupId = groupId.takeIf { it.isNotEmpty() },
                        replacedWalletId = (activity as? MembershipActivity)?.onChainReplaceWalletId,
                        quickWalletParam = quickWalletParam
                    )
                    NavOptions.Builder()
                        .setPopUpTo(findNavController().graph.startDestinationId, true)
                        .build()
                }
            }
        }
    }

    private fun handleCreateWalletSuccess(event: PrimaryOwnerEvent.OnCreateWalletSuccess) {
        if (event.airgapCount > 0) {
            findNavController().navigate(
                PrimaryOwnerFragmentDirections.actionPrimaryOwnerFragmentToRegisterWalletToAirgapFragment(
                    walletId = event.wallet.id,
                    sendBsmsEmail = args.sendBsmsEmail
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        } else if (!args.sendBsmsEmail) {
            launcher.launch(
                navigator.buildBackupWalletIntent(
                    activityContext = requireActivity(),
                    args = BackUpWalletArgs(
                        wallet = event.wallet,
                        backUpWalletType = BackUpWalletType.ASSISTED_CREATED
                    )
                )
            )
        } else {
            val replacedWalletId = (activity as? com.nunchuk.android.main.membership.MembershipActivity)?.onChainReplaceWalletId.orEmpty()
            findNavController().navigate(
                PrimaryOwnerFragmentDirections.actionPrimaryOwnerFragmentToCreateWalletSuccessFragment(
                    event.wallet.id,
                    replacedWalletId
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        }
    }
}

@Composable
private fun PrimaryOwnerScreen(
    viewModel: PrimaryOwnerViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    flow: Int = PrimaryOwnerFlow.NONE,
    onSkipClick: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

    PrimaryOwnerContent(
        flow = flow,
        state = state,
        remainTime = remainTime,
        enableContinueButton = viewModel.enableContinueButton(),
        onSelectMember = {
            viewModel.updateSelectMember(it)
        },
        onSkipClick = onSkipClick,
        onContinueClick = {
            viewModel.onContinueClick()
        },
        onMoreClicked = onMoreClicked
    )
}

@Composable
private fun PrimaryOwnerContent(
    state: PrimaryOwnerState,
    enableContinueButton: Boolean = false,
    flow: Int = PrimaryOwnerFlow.NONE,
    remainTime: Int = 0,
    onContinueClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
    onSelectMember: (ByzantineMember) -> Unit = { _ -> },
    onMoreClicked: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var dropdownSize by remember { mutableStateOf(Size.Zero) }
    fun onDropdownDismissRequest() {
        expanded = false
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (flow != PrimaryOwnerFlow.EDIT) stringResource(
                        R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                    isBack = flow != PrimaryOwnerFlow.EDIT,
                    actions = {
                        if (flow != PrimaryOwnerFlow.EDIT) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier.size(
                                    LocalViewConfiguration.current.minimumTouchTargetSize
                                )
                            )
                        }
                    })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_primary_owner),
                        style = NunchukTheme.typography.heading
                    )

                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(id = R.string.nc_primary_owner_desc),
                        style = NunchukTheme.typography.body
                    )

                    Column {
                        NcTextField(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    dropdownSize = coordinates.size.toSize()
                                },
                            value = state.member?.getDisplayName().orEmpty(),
                            enabled = false,
                            disableBackgroundColor = MaterialTheme.colorScheme.surface,
                            onClick = {
                                expanded = true
                            },
                            title = stringResource(id = R.string.nc_primary_owner),
                            onValueChange = { },
                        )

                        DropdownMenu(
                            modifier = Modifier
                                .width(with(LocalDensity.current) { dropdownSize.width.toDp() }),
                            expanded = expanded,
                            properties = PopupProperties(
                                focusable = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            ),
                            onDismissRequest = { onDropdownDismissRequest() }
                        ) {
                            state.members.mapNotNull { it.user }
                                .forEach { user ->
                                    DropdownMenuItem(
                                        modifier = Modifier.padding(top = 8.dp),
                                        onClick = {
                                            onSelectMember(state.members.first { it.user == user })
                                            onDropdownDismissRequest()
                                        }, text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp, 48.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            color = colorResource(
                                                                id = R.color.nc_beeswax_light
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = user.name.shorten(),
                                                        style = NunchukTheme.typography.heading
                                                    )
                                                }

                                                Column(
                                                    modifier = Modifier.padding(start = 12.dp),
                                                    verticalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    Text(
                                                        text = user.name,
                                                        style = NunchukTheme.typography.body
                                                    )
                                                    Text(
                                                        text = user.email,
                                                        style = NunchukTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        })
                                }
                        }
                    }
                }

                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_primary_owner_hint)))
                )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = enableContinueButton,
                    onClick = onContinueClick
                ) {
                    val text = if (flow == PrimaryOwnerFlow.EDIT) {
                        stringResource(id = R.string.nc_text_save)
                    } else {
                        stringResource(id = R.string.nc_text_continue)
                    }
                    Text(text = text)
                }

                if (flow != PrimaryOwnerFlow.EDIT) {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        onClick = onSkipClick,
                    ) {
                        Text(
                            modifier = Modifier.padding(start = 6.dp),
                            text = stringResource(id = R.string.nc_text_skip)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMemberView() {
    PrimaryOwnerContent(state = PrimaryOwnerState())
}