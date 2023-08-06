package com.nunchuk.android.main.membership.byzantine.initvitemember

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
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
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.ByzantineMemberFlow
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardViewModel
import com.nunchuk.android.main.membership.byzantine.selectrole.ByzantineSelectRoleFragment
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                InviteMembersScreen(flow = args.flow, viewModel = viewModel, onSelectRole = {
                    findNavController().navigate(
                        ByzantineInviteMembersFragmentDirections.actionByzantineInviteMembersFragmentToByzantineSelectRoleFragment(
                            role = it
                        )
                    )
                }, onContinueClick = {
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
                }, onMoreClicked = ::handleShowMore)
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
                is ByzantineInviteMembersEvent.CreateGroupWalletSuccess -> {
                    navigator.openMembershipActivity(
                        activityContext = requireActivity(),
                        groupStep = MembershipStage.NONE,
                        groupId = event.groupId
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
            }
        }
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
                message = getString(R.string.nc_admin_role_message_dialog),
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
    onSelectRole: (String) -> Unit = { _ -> },
    onContinueClick: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InviteMembersContent(
        flow = flow,
        members = state.members,
        preMembers = state.preMembers,
        suggestionContacts = state.suggestionContacts,
        enableContinueButton = viewModel.enableContinueButton(),
        onAddMember = { viewModel.addMember() },
        onSelectRole = { index, role ->
            viewModel.interactingMemberIndex(index)
            onSelectRole(role)
        },
        onInputEmailChange = { index, email, name, loginType ->
            viewModel.updateMember(index, email = email, name = name, loginType = loginType)
        },
        onRemoveMember = { viewModel.removeMember(it) },
        onContinueClick = onContinueClick,
        onMoreClicked = onMoreClicked
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InviteMembersContent(
    members: List<InviteMemberUi> = emptyList(),
    preMembers: List<InviteMemberUi> = emptyList(),
    suggestionContacts: List<Contact> = emptyList(),
    selectContact: HashSet<String> = hashSetOf(),
    enableContinueButton: Boolean = false,
    flow: Int = ByzantineMemberFlow.NONE,
    onRemoveMember: (Int) -> Unit = {},
    onContinueClick: () -> Unit = {},
    onAddMember: () -> Unit = {},
    onSelectRole: (Int, String) -> Unit = { _, _ -> },
    onInputEmailChange: (Int, String, String, String) -> Unit = { _, _, _, _ -> },
    onSelectContact: (String) -> Unit = {},
    onMoreClicked: () -> Unit = {}
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
                    elevation = 0.dp,
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
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
                    NcHighlightText(
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_invite_members_desc),
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
                            selectContact = selectContact,
                            isPreMember = preMembers.find { it.email == member.email } != null,
                            onSelectRoleClick = {
                                onSelectRole(index, member.role)
                            },
                            onRemoveClick = {
                                onRemoveMember(index)
                            },
                            onInputEmailChange = { email, name, loginType ->
                                onInputEmailChange(index, email, name, loginType)
                            },
                            onSelectContact = {
                                onSelectContact(it)
                            },
                            onFocusEvent = {
                                if (it.isFocused) {
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
                                Image(
                                    painterResource(id = R.drawable.ic_plus_dark),
                                    contentDescription = null,
                                )

                                Text(
                                    modifier = Modifier.padding(start = 6.dp),
                                    text = stringResource(id = R.string.nc_add_more_members)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
private fun MemberView(
    index: Int = 0,
    email: String = "",
    name: String = "",
    error: String = "",
    isPreMember: Boolean = false,
    suggestionContacts: List<Contact> = emptyList(),
    selectContact: HashSet<String> = hashSetOf(),
    role: String = AssistedWalletRole.NONE.name,
    onRemoveClick: () -> Unit = {},
    onInputEmailChange: (String, String, String) -> Unit = { _, _, _ -> },
    onSelectRoleClick: () -> Unit = {},
    onSelectContact: (String) -> Unit = {},
    onFocusEvent: (FocusState) -> Unit = {},
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
                    color = colorResource(id = R.color.nc_primary_color)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(12.dp))
                .border(width = 1.dp, color = NcColor.border, shape = RoundedCornerShape(12.dp))
                .background(color = NcColor.greyLight)
                .padding(16.dp)
        ) {
            Column {
                if (isMaster || isPreMember) {
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
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        if (email in selectContact) {
                            Column {
                                Text(
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    text = stringResource(id = R.string.nc_email_address),
                                    style = NunchukTheme.typography.titleSmall
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = NcColor.border,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(color = NcColor.greyLight)
                                        .padding(16.dp)
                                ) {
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
                                        }
                                    }
                                }
                            }
                        } else {
                            NcTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        dropdownSize = coordinates.size.toSize()
                                    },
                                value = email,
                                onValueChange = {
                                    onInputEmailChange(it.trim(), "", "")
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
                                                onInputEmailChange(contact.email, contact.name, contact.loginType)
                                                onSelectContact(contact.email)
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
                }
                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    title = stringResource(id = R.string.nc_role),
                    value = role.toTitle(stringResource(id = R.string.nc_select_a_role)),
                    enabled = false,
                    disableBackgroundColor = if (isMaster) colorResource(id = R.color.nc_whisper_color) else MaterialTheme.colors.surface,
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

@Preview
@Composable
private fun InviteMembersScreenPreview() {
    InviteMembersScreen()
}

@Preview
@Composable
private fun PreviewMemberView() {
    MemberView(
        index = 1,
        email = "boblee@gmail.com",
        name = "Bob Lee",
        role = AssistedWalletRole.MASTER.name,
    )
}