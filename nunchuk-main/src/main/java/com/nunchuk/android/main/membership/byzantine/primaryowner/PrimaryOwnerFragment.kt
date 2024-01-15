package com.nunchuk.android.main.membership.byzantine.primaryowner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import com.nunchuk.android.main.membership.key.AddKeyStepViewModel
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PrimaryOwnerFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: PrimaryOwnerViewModel by viewModels()
    private val args: PrimaryOwnerFragmentArgs by navArgs()
    private val addKeyStepViewModel: AddKeyStepViewModel by activityViewModels()

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
                    addKeyStepViewModel.requireInheritance(event.walletId)
                    handleCreateWalletSuccess(event)
                }
            }
        }
    }

    private fun handleCreateWalletSuccess(event: PrimaryOwnerEvent.OnCreateWalletSuccess) {
        if (event.airgapCount > 0) {
            findNavController().navigate(
                PrimaryOwnerFragmentDirections.actionPrimaryOwnerFragmentToRegisterWalletToAirgapFragment(
                    event.walletId,
                ),
                NavOptions.Builder()
                    .setPopUpTo(findNavController().graph.startDestinationId, true)
                    .build()
            )
        } else {
            findNavController().navigate(
                PrimaryOwnerFragmentDirections.actionPrimaryOwnerFragmentToCreateWalletSuccessFragment(
                    event.walletId
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
        onInputEmailChange = { email ->
            viewModel.updateEmail(email)
        },
        onSkipClick = onSkipClick,
        onContinueClick = {
            viewModel.onContinueClick()
        },
        onMoreClicked = onMoreClicked
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun PrimaryOwnerContent(
    state: PrimaryOwnerState,
    enableContinueButton: Boolean = false,
    flow: Int = PrimaryOwnerFlow.NONE,
    remainTime: Int = 0,
    onContinueClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
    onInputEmailChange: (String) -> Unit = { _ -> },
    onMoreClicked: () -> Unit = {},
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var dropdownSize by remember { mutableStateOf(Size.Zero) }
    val keyboardController = LocalSoftwareKeyboardController.current
    fun onDropdownDismissRequest() {
        expanded = false
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding(),
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
                            value = state.email,
                            onValueChange = {
                                if (it.trim() != state.email) onInputEmailChange(it.trim())
                                expanded = true
                            },
                            title = stringResource(id = R.string.nc_primary_owner),
                            onFocusEvent = {
                                if (it.isFocused) {
                                    coroutineScope.launch {
                                        delay(500L)
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                            })
                        )

                        DropdownMenu(
                            modifier = Modifier
                                .width(with(LocalDensity.current) { dropdownSize.width.toDp() })
                                .heightIn(max = 125.dp),
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
                                            if (user.email != state.email) {
                                                onInputEmailChange(user.email)
                                            }
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