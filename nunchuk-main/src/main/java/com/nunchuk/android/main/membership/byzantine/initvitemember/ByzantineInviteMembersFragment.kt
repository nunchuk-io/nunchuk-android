package com.nunchuk.android.main.membership.byzantine.initvitemember

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardViewModel
import com.nunchuk.android.main.membership.byzantine.selectrole.ByzantineSelectRoleFragment
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.byzantine.toGroupWalletType
import com.nunchuk.android.model.byzantine.toTitle
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ByzantineInviteMembersFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ByzantineInviteMembersViewModel by viewModels()
    private val args: ByzantineInviteMembersFragmentArgs by navArgs()
    private val groupDashboardViewModel: GroupDashboardViewModel by activityViewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val signatureMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                        ?: return@registerForActivityResult
                val securityQuestionToken =
                    data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                val confirmCodeMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.CONFIRM_CODE)
                        .orEmpty()
                viewModel.editGroupMember(
                    signatureMap,
                    securityQuestionToken,
                    confirmCodeMap[GlobalResultKey.CONFIRM_CODE_TOKEN].orEmpty(),
                    confirmCodeMap[GlobalResultKey.CONFIRM_CODE_NONCE].orEmpty()
                )
            }
        }

    override val allowRestartWizard: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InviteMembersScreen(
                    flow = args.flow, viewModel = viewModel,
                    onSelectRole = {
                        findNavController().navigate(
                            ByzantineInviteMembersFragmentDirections.actionByzantineInviteMembersFragmentToByzantineSelectRoleFragment(
                                role = it,
                                groupType = args.groupType
                            )
                        )
                    },
                    onContinueClick = {
                        if (viewModel.isExcessKeyholderLimit()) {
                            showLimitKeyholderDialog()
                            return@InviteMembersScreen
                        }
                        if (args.flow == ByzantineMemberFlow.SETUP) {
                            val onCreateGroupWallet: () -> Unit = {
                                viewModel.createGroup()
                            }
                            if (viewModel.hasAdminRole()) {
                                showAdminRoleDialog(onCreateGroupWallet)
                            } else {
                                onCreateGroupWallet()
                            }
                        } else {
                            enterPasswordDialog()
                        }
                    },
                    onMoreClicked = ::handleShowMore,
                    groupWalletType = args.groupType.toGroupWalletType(),
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(ByzantineSelectRoleFragment.REQUEST_KEY) { _, bundle ->
            val role = bundle.getString(ByzantineSelectRoleFragment.EXTRA_ROLE)
            viewModel.updateMember(viewModel.getInteractingMemberIndex(), role = role)
        }
        flowObserver(viewModel.event) { event ->
            when (event) {
                is ByzantineInviteMembersEvent.Error -> showError(message = event.message)
                is ByzantineInviteMembersEvent.Loading -> showOrHideLoading(event.loading)
                ByzantineInviteMembersEvent.LimitKeyholderRoleWarning -> showLimitKeyholderDialog()
                is ByzantineInviteMembersEvent.CreateGroupWalletSuccess -> {
                    navigator.openMembershipActivity(
                        activityContext = requireActivity(),
                        groupStep = MembershipStage.NONE,
                        groupId = event.groupId,
                        isPersonalWallet = false,
                        walletType = args.groupType.toGroupWalletType()
                    )
                    requireActivity().finish()
                }

                is ByzantineInviteMembersEvent.CalculateRequiredSignaturesSuccess -> {
                    navigator.openWalletAuthentication(
                        walletId = "",
                        userData = event.userData,
                        requiredSignatures = event.requiredSignatures,
                        type = event.type,
                        action = TargetAction.EDIT_GROUP_MEMBERS.name,
                        launcher = launcher,
                        activityContext = requireActivity()
                    )
                }

                is ByzantineInviteMembersEvent.EditGroupMemberSuccess -> {
                    groupDashboardViewModel.setByzantineMembers(event.members)
                    showSuccess(message = getString(R.string.nc_members_updated))
                }

                ByzantineInviteMembersEvent.RemoveMemberInheritanceWarning -> showRemoveMemberInheritanceDialog()
                is ByzantineInviteMembersEvent.FacilitatorAdminWarning -> {
                    NCInfoDialog(requireActivity()).showDialog(message = event.message)
                }
            }
        }
    }

    private fun showLimitKeyholderDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            message = String.format(
                getString(R.string.nc_limit_keyholder_message_dialog),
                viewModel.getMaximumKeyholderRole().toString()
            )
        )
    }

    private fun showRemoveMemberInheritanceDialog() {
        NCInfoDialog(requireActivity()).showDialog(message = getString(R.string.nc_remove_member_inheritance_message_dialog))
    }

    private fun enterPasswordDialog() {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                viewModel.confirmPassword(it)
            }
        )
    }

    private fun showAdminRoleDialog(onContinueClick: () -> Unit) {
        NCWarningDialog(requireActivity())
            .showDialog(
                message = if (viewModel.allowInheritance())
                    getString(R.string.nc_admin_role_message_dialog) else
                    getString(R.string.nc_admin_role_no_inheritance_message_dialog),
                btnYes = getString(R.string.nc_text_got_it),
                btnNo = getString(R.string.nc_cancel),
                onYesClick = {
                    onContinueClick()
                },
                onNoClick = {
                    viewModel.clearAdminRole()
                }
            )
    }
}

@Composable
private fun InviteMembersScreen(
    viewModel: ByzantineInviteMembersViewModel = viewModel(),
    flow: Int = ByzantineMemberFlow.NONE,
    groupWalletType: GroupWalletType? = null,
    onSelectRole: (String) -> Unit = { _ -> },
    onContinueClick: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InviteMembersContent(
        flow = flow,
        members = state.members,
        suggestionContacts = state.suggestionContacts,
        enableContinueButton = viewModel.enableContinueButton(),
        onAddMember = { viewModel.addMember() },
        onSelectRole = { index, role ->
            viewModel.interactingMemberIndex(index)
            onSelectRole(role)
        },
        onInputEmailChange = { index, email, name ->
            viewModel.updateMember(index, email = email, name = name)
        },
        onRemoveMember = { viewModel.removeMember(it) },
        onContinueClick = onContinueClick,
        onMoreClicked = onMoreClicked,
        groupWalletType = groupWalletType
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InviteMembersContent(
    members: List<InviteMemberUi> = emptyList(),
    suggestionContacts: List<Contact> = emptyList(),
    enableContinueButton: Boolean = false,
    flow: Int = ByzantineMemberFlow.NONE,
    groupWalletType: GroupWalletType? = null,
    onRemoveMember: (Int) -> Unit = {},
    onContinueClick: () -> Unit = {},
    onAddMember: () -> Unit = {},
    onSelectRole: (Int, String) -> Unit = { _, _ -> },
    onInputEmailChange: (Int, String, String) -> Unit = { _, _, _ -> },
    onMoreClicked: () -> Unit = {},
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = enableContinueButton,
                    onClick = onContinueClick
                ) {
                    val text = if (flow == ByzantineMemberFlow.EDIT) {
                        stringResource(id = R.string.nc_text_save)
                    } else {
                        stringResource(id = R.string.nc_text_continue)
                    }
                    Text(text = text)
                }
            }, topBar = {
                val title = when (flow) {
                    ByzantineMemberFlow.EDIT -> stringResource(R.string.nc_members)
                    else -> ""
                }
                NcTopAppBar(
                    title = title,
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = flow != ByzantineMemberFlow.EDIT,
                    actions = {
                        if (flow != ByzantineMemberFlow.EDIT) {
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
                if (flow == ByzantineMemberFlow.SETUP) {
                    Text(
                        modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_invite_members),
                        style = NunchukTheme.typography.heading
                    )
                    val desc = when (groupWalletType) {
                        GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY, GroupWalletType.THREE_OF_FIVE_INHERITANCE,
                        -> stringResource(R.string.nc_invite_members_3_of_5_pro_desc)

                        GroupWalletType.THREE_OF_FIVE -> stringResource(R.string.nc_invite_members_3_of_5_standard_desc)
                        else -> stringResource(R.string.nc_invite_members_desc)
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = desc,
                        style = NunchukTheme.typography.body
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(members) { index, member ->
                        MemberView(index = index + 1,
                            email = member.email,
                            name = member.name.orEmpty(),
                            role = member.role,
                            error = member.err.orEmpty(),
                            suggestionContacts = suggestionContacts,
                            isContact = member.isContact,
                            isNewAdded = member.isNewAdded,
                            onSelectRoleClick = {
                                onSelectRole(index, member.role)
                            },
                            onRemoveClick = {
                                onRemoveMember(index)
                            },
                            onInputEmailChange = { email, name ->
                                onInputEmailChange(index, email, name)
                            },
                            onFocusEvent = {
                                if (it) {
                                    coroutineScope.launch {
                                        delay(500L)
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            })
                    }
                    item {
                        NcOutlineButton(
                            modifier = Modifier
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp),
                            onClick = onAddMember,
                        ) {
                            Row {
                                NcIcon(
                                    painterResource(id = R.drawable.ic_plus_dark),
                                    contentDescription = null,
                                )

                                Text(
                                    modifier = Modifier.padding(start = 6.dp),
                                    text = stringResource(id = R.string.nc_add_member)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MemberView(
    index: Int = 0,
    email: String = "",
    name: String = "",
    error: String = "",
    isContact: Boolean = false,
    isNewAdded: Boolean = false,
    suggestionContacts: List<Contact> = emptyList(),
    role: String = AssistedWalletRole.NONE.name,
    onRemoveClick: () -> Unit = {},
    onInputEmailChange: (String, String) -> Unit = { _, _ -> },
    onSelectRoleClick: () -> Unit = {},
    onFocusEvent: (Boolean) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val isMaster = role == AssistedWalletRole.MASTER.name
    var dropdownSize by remember { mutableStateOf(Size.Zero) }
    val keyboardController = LocalSoftwareKeyboardController.current
    fun onDropdownDismissRequest() {
        expanded = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            val title = if (index == 1) stringResource(id = R.string.nc_master) else stringResource(
                id = R.string.nc_member_data,
                index
            )
            Text(
                text = title,
                style = NunchukTheme.typography.title
            )
            if (isMaster.not()) {
                Text(
                    modifier = Modifier.clickable {
                        onRemoveClick()
                    },
                    text = stringResource(id = R.string.nc_remove),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline,
                    color = colorResource(id = R.color.nc_text_primary)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(12.dp))
                .border(width = 1.dp, color = MaterialTheme.colorScheme.strokePrimary, shape = RoundedCornerShape(12.dp))
                .background(color = MaterialTheme.colorScheme.greyLight)
                .padding(16.dp)
        ) {
            Column {
                if (isMaster || isContact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 48.dp)
                                .clip(CircleShape)
                                .background(color = colorResource(id = R.color.nc_beeswax_light)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.shorten(),
                                style = NunchukTheme.typography.heading
                            )
                        }

                        Column(
                            modifier = Modifier.padding(start = 12.dp),
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(text = name, style = NunchukTheme.typography.body)
                            Text(
                                text = email,
                                style = NunchukTheme.typography.bodySmall
                            )
                        }
                    }
                } else if (isContact.not() && isNewAdded.not()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .weight(1f, false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp, 48.dp)
                                    .clip(CircleShape)
                                    .background(color = colorResource(id = R.color.nc_bg_mid_gray)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_account_member),
                                    contentDescription = ""
                                )
                            }

                            Text(
                                modifier = Modifier
                                    .padding(start = 12.dp),
                                text = email,
                                style = NunchukTheme.typography.body,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )

                        }
                        Text(
                            text = "Pending",
                            style = NunchukTheme.typography.title
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        NcTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    dropdownSize = coordinates.size.toSize()
                                },
                            value = email,
                            onValueChange = {
                                if (it.trim() != email) onInputEmailChange(it.trim(), "")
                                expanded = true
                            },
                            title = stringResource(id = R.string.nc_email_address),
                            error = error,
                            onFocusEvent = onFocusEvent,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                            })
                        )

                        if (email.isNotEmpty() && suggestionContacts.isNotEmpty()) {
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
                                suggestionContacts.forEach { contact ->
                                    DropdownMenuItem(
                                        modifier = Modifier.padding(top = 8.dp),
                                        onClick = {
                                            if (contact.email != email) {
                                                onInputEmailChange(contact.email, contact.name)
                                            }
                                            onDropdownDismissRequest()
                                        }, text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp, 48.dp)
                                                        .clip(CircleShape)
                                                        .background(color = colorResource(id = R.color.nc_beeswax_light)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = contact.name.shorten(),
                                                        style = NunchukTheme.typography.heading
                                                    )
                                                }

                                                Column(
                                                    modifier = Modifier.padding(start = 12.dp),
                                                    verticalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    Text(
                                                        text = contact.name,
                                                        style = NunchukTheme.typography.body
                                                    )
                                                    Text(
                                                        text = contact.email,
                                                        style = NunchukTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        })
                                }
                            }
                        }
                    }
                }
                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    title = stringResource(id = R.string.nc_role),
                    value = role.toTitle(stringResource(id = R.string.nc_select_a_role)),
                    enabled = false,
                    disableBackgroundColor = if (isMaster) colorResource(id = R.color.nc_bg_mid_gray) else MaterialTheme.colorScheme.surface,
                    onClick = {
                        if (isMaster.not()) onSelectRoleClick()
                    },
                    rightContent = {
                        if (isMaster.not()) {
                            Image(
                                modifier = Modifier
                                    .padding(end = 12.dp),
                                painter = painterResource(id = R.drawable.ic_arrow),
                                contentDescription = ""
                            )
                        }
                    },
                    onValueChange = {}
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewMemberView() {
    MemberView(
        index = 1,
        email = "boblee@gmail.com",
        name = "Bob Lee",
        role = AssistedWalletRole.MASTER.name,
    )
}