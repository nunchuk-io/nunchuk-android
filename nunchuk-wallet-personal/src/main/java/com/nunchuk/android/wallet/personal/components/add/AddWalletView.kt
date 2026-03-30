package com.nunchuk.android.wallet.personal.components.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.data.model.GroupWalletDataComposer
import com.nunchuk.android.core.data.model.WalletConfigType
import com.nunchuk.android.core.data.model.getMN
import com.nunchuk.android.core.data.model.getWalletConfigTypeBy
import com.nunchuk.android.core.data.model.toOptionName
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.core.miniscript.SelectMultisignTypeBottomSheet
import com.nunchuk.android.core.miniscript.formatMiniscript
import com.nunchuk.android.model.GlobalGroupWalletConfig
import com.nunchuk.android.model.wallet.GroupInvitation
import com.nunchuk.android.nav.args.MiniscriptArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.wallet.personal.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletView(
    state: AddWalletState,
    viewModel: AddWalletViewModel,
    isCreateMiniscriptWallet: Boolean = false,
    viewOnlyComposer: GroupWalletDataComposer? = null,
    isEditGroupWallet: Boolean,
    isViewConfigOnly: Boolean = true,
    showInviteSection: Boolean = false,
    onSelectAddressType: (AddressType) -> Unit = {},
    onSendInvite: (List<String>) -> Unit = {},
    onRemoveInvite: (String) -> Unit = {},
    onContinue: (String, AddressType, Int, Int, WalletConfigType) -> Unit = { _, _, _, _, _ -> },
    onNavigateToMiniscript: (MiniscriptArgs) -> Unit = {}
) {
    var walletName by rememberSaveable { mutableStateOf("") }
    var viewAll by rememberSaveable { mutableStateOf(false) }
    var walletConfigType by rememberSaveable {
        mutableStateOf(WalletConfigType.TOW_OF_THREE)
    }
    var isWalletConfigManuallySelected by rememberSaveable { mutableStateOf(false) }
    var keys by remember { mutableIntStateOf(0) }
    var requiredKeys by remember { mutableIntStateOf(0) }
    var showMiniscriptBottomSheet by remember { mutableStateOf(false) }
    var showInviteBottomSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get miniscript data from state
    val miniscriptTemplate = state.miniscriptTemplate

    val isEditModeHasConfigChanges = if (isEditGroupWallet) {
        val groupSandbox = state.groupSandbox
        if (groupSandbox == null) {
            false
        } else {
            val effectiveConfig = when (walletConfigType) {
                WalletConfigType.CUSTOM -> requiredKeys to keys
                WalletConfigType.MINISCRIPT -> groupSandbox.m to groupSandbox.n
                else -> walletConfigType.getMN().first to walletConfigType.getMN().second
            }
            val currentMiniscriptTemplate =
                if (walletConfigType == WalletConfigType.MINISCRIPT) miniscriptTemplate else ""

            walletName != groupSandbox.name
                || state.addressTypeSelected != groupSandbox.addressType
                || effectiveConfig.first != groupSandbox.m
                || effectiveConfig.second != groupSandbox.n
                || currentMiniscriptTemplate != groupSandbox.miniscriptTemplate
        }
    } else {
        true
    }

    val isContinueEnabled = walletName.isNotBlank() &&
        isEditModeHasConfigChanges &&
        (walletConfigType != WalletConfigType.MINISCRIPT || miniscriptTemplate.isNotEmpty())

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        Timber.tag("miniscript-feature").d("File picked: $uri")
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = BufferedReader(inputStream.reader()).use { reader ->
                        reader.readText()
                    }
                    // Navigate to miniscript import flow with the file content
                    onNavigateToMiniscript(
                        MiniscriptArgs(
                            walletName = walletName,
                            addressType = state.addressTypeSelected,
                            fromAddWallet = true,
                            multisignType = MultisignType.IMPORT,
                            template = MiniscriptUtil.revertFormattedMiniscript(content)
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e("Error reading file: $e")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        NcSnackbarVisuals(
                            message = "Failed to read data or script has invalid syntax",
                            type = NcToastType.ERROR
                        )
                    )
                }
            }
        }
    }

    if (state.isLoading) {
        NcLoadingDialog()
    }

    LaunchedEffect(state.groupSandbox?.name, viewOnlyComposer?.walletName) {
        walletName = viewOnlyComposer?.walletName ?: (state.groupSandbox?.name ?: "")
    }

    LaunchedEffect(state.groupSandbox?.n ?: 3, state.groupSandbox?.m ?: 2, miniscriptTemplate) {
        if (miniscriptTemplate.isEmpty() && walletConfigType != WalletConfigType.MINISCRIPT && !isWalletConfigManuallySelected) {
            walletConfigType = getWalletConfigTypeBy(
                n = state.groupSandbox?.n ?: 3,
                m = state.groupSandbox?.m ?: 2
            )
            requiredKeys = walletConfigType.getMN().first
            keys = walletConfigType.getMN().second
        }
    }

    LaunchedEffect(miniscriptTemplate) {
        if (miniscriptTemplate.isNotEmpty()) {
            walletConfigType = WalletConfigType.MINISCRIPT
        }
    }

    val options = if (!viewAll) listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT
    ) else if (isCreateMiniscriptWallet) listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT
    ) else listOf(
        AddressType.NATIVE_SEGWIT,
        AddressType.TAPROOT,
        AddressType.NESTED_SEGWIT,
        AddressType.LEGACY
    )

    val walletConfigOptions = remember { WalletConfigType.entries }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100L)
        focusRequester.requestFocus()
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .systemBarsPadding()
                .imePadding()
                .fillMaxSize(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_config),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = isContinueEnabled,
                    onClick = {
                        onContinue(
                            walletName,
                            state.addressTypeSelected,
                            requiredKeys,
                            keys,
                            walletConfigType
                        )
                    }
                ) {
                    Text(
                        text = if (isEditGroupWallet || viewOnlyComposer != null) stringResource(id = R.string.nc_save_changes) else stringResource(
                            id = R.string.nc_text_continue
                        ),
                        style = NunchukTheme.typography.title.copy(
                            color = MaterialTheme.colorScheme.controlTextPrimary
                        )
                    )
                }
            },
            snackbarHost = {
                NcSnackBarHost(snackbarHostState)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .animateContentSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                NcTextField(
                    title = stringResource(id = R.string.nc_text_wallet_name),
                    value = walletName,
                    onValueChange = { walletName = it.take(80) },
                    enableMaxLength = true,
                    maxLength = 80,
                    modifier = Modifier.focusRequester(focusRequester)
                )

                Text(
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                    text = stringResource(id = R.string.nc_wallet_address_type),
                    style = NunchukTheme.typography.titleSmall
                )

                options.forEach { type ->
                    val isDisabled = walletConfigType == WalletConfigType.MINISCRIPT && 
                        (type == AddressType.NESTED_SEGWIT || type == AddressType.LEGACY)
                    
                    TypeOption(
                        isViewOnly = isViewConfigOnly && type != viewOnlyComposer?.addressType,
                        selected = if (viewOnlyComposer != null) viewOnlyComposer.addressType == type else state.addressTypeSelected == type,
                        name = getAddressType(type),
                        badge = getBadge(type),
                        disabled = isDisabled,
                        onClick = {
                            if (!isDisabled) {
                                onSelectAddressType(type)
                            }
                        }
                    )
                }

                if (!viewAll && !isCreateMiniscriptWallet) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = { viewAll = !viewAll })
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(R.string.nc_view_all),
                            style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
                        )

                        NcIcon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(id = R.drawable.ic_dropdown_outline),
                            contentDescription = null,
                        )
                    }
                }

                if ((isEditGroupWallet && state.groupSandbox != null) || isViewConfigOnly) {
                    if (!isCreateMiniscriptWallet) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.backgroundMidGray
                        )

                        Text(
                            text = "Wallet config",
                            modifier = Modifier.padding(bottom = 6.dp),
                            style = NunchukTheme.typography.titleSmall
                        )

                        walletConfigOptions.filter {
                            // Hide MINISCRIPT when it is currently selected
                            if (walletConfigType == WalletConfigType.MINISCRIPT && it == WalletConfigType.MINISCRIPT) {
                                false
                            } else {
                                true
                            }
                        }.forEach { option ->
                            TypeOption(
                                isViewOnly = isViewConfigOnly && option != viewOnlyComposer?.walletConfigType,
                                selected = if (viewOnlyComposer != null) viewOnlyComposer.walletConfigType == option else walletConfigType == option,
                                name = option.toOptionName(),
                                badge = if (option == WalletConfigType.MINISCRIPT) "New" else null,
                                isEndItem = option == WalletConfigType.CUSTOM && walletConfigType != WalletConfigType.CUSTOM,
                                onClick = {
                                    val previousWalletType = walletConfigType
                                    walletConfigType = option
                                    isWalletConfigManuallySelected = true
                                    
                                    if (option != WalletConfigType.CUSTOM && option != WalletConfigType.MINISCRIPT) {
                                        requiredKeys = option.getMN().first
                                        keys = option.getMN().second
                                    }
                                    
                                    // If switching to miniscript and current address type is incompatible, switch to TAPROOT
                                    if (option == WalletConfigType.MINISCRIPT && 
                                        (state.addressTypeSelected == AddressType.NESTED_SEGWIT || 
                                         state.addressTypeSelected == AddressType.LEGACY)) {
                                        onSelectAddressType(AddressType.TAPROOT)
                                    }
                                    
                                    // Clear miniscript template if switching away from miniscript wallet type
                                    if (previousWalletType == WalletConfigType.MINISCRIPT && option != WalletConfigType.MINISCRIPT) {
                                        viewModel.clearMiniscriptTemplate()
                                    }
                                }
                            )
                            
                            // Render KeysAndRequiredKeysScreen right after CUSTOM option when selected
                            if (option == WalletConfigType.CUSTOM && walletConfigType == WalletConfigType.CUSTOM && !isCreateMiniscriptWallet) {
                                KeysAndRequiredKeysScreen(
                                    state.freeGroupWalletConfig,
                                    m = viewOnlyComposer?.requireKeys ?: state.groupSandbox?.m?.takeIf { it > 0 } ?: 1,
                                    n = viewOnlyComposer?.totalKeys ?: state.groupSandbox?.n?.takeIf { it > 0 } ?: 1,
                                    viewOnly = isViewConfigOnly,
                                ) { m, n ->
                                    keys = n
                                    requiredKeys = m
                                }
                            }
                        }
                    }
                }

                if (walletConfigType == WalletConfigType.MINISCRIPT) {
                    if (miniscriptTemplate.isNotEmpty()) {
                        MiniscriptSectionFilled(
                            miniscriptContent = miniscriptTemplate.formatMiniscript(),
                            onEdit = {
                                onNavigateToMiniscript(
                                    MiniscriptArgs(
                                        walletName = walletName,
                                        addressType = state.addressTypeSelected,
                                        fromAddWallet = true,
                                        multisignType = MultisignType.CUSTOM,
                                        template = miniscriptTemplate
                                    )
                                )
                            },
                            onRemove = {
                                viewModel.clearMiniscriptTemplate()
                            }
                        )
                    } else {
                        MiniscriptSection(onAddMiniscript = { showMiniscriptBottomSheet = true })
                    }
                }

                if (showInviteSection) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                        color = MaterialTheme.colorScheme.backgroundMidGray
                    )
                    InviteSection(
                        invitations = state.groupInvitations,
                        onAddInvite = { showInviteBottomSheet = true },
                        onRemoveInvite = onRemoveInvite,
                    )
                }
            }
        }

        if (showMiniscriptBottomSheet) {
            SelectMultisignTypeBottomSheet(
                onSelect = { multisignType ->
                    when (multisignType) {
                        MultisignType.IMPORT -> {
                            filePickerLauncher.launch("text/*")
                        }

                        MultisignType.CUSTOM -> {
                            onNavigateToMiniscript(
                                MiniscriptArgs(
                                    walletName = walletName,
                                    addressType = state.addressTypeSelected,
                                    fromAddWallet = true,
                                    multisignType = MultisignType.CUSTOM
                                )
                            )
                        }

                        else -> {
                            // For other multisign types (FLEXIBLE, EXPANDING, DECAYING)
                            onNavigateToMiniscript(
                                MiniscriptArgs(
                                    walletName = walletName,
                                    addressType = state.addressTypeSelected,
                                    fromAddWallet = true,
                                    multisignType = multisignType
                                )
                            )
                        }
                    }
                    showMiniscriptBottomSheet = false
                },
                onDismiss = {
                    showMiniscriptBottomSheet = false
                }
            )
        }

        if (showInviteBottomSheet) {
            InviteEmailsBottomSheet(
                onDismiss = { showInviteBottomSheet = false },
                onSendInvite = { emails ->
                    onSendInvite(emails)
                    showInviteBottomSheet = false
                },
            )
        }
    }
}

@Composable
fun getAddressType(type: AddressType): String {
    return when (type) {
        AddressType.LEGACY -> stringResource(id = R.string.nc_wallet_legacy_wallet)
        AddressType.NATIVE_SEGWIT -> stringResource(id = R.string.nc_wallet_native_segwit_wallet)
        AddressType.NESTED_SEGWIT -> stringResource(id = R.string.nc_wallet_nested_segwit_wallet)
        AddressType.TAPROOT -> stringResource(id = R.string.nc_wallet_taproot_wallet)
        else -> ""
    }
}

@Composable
fun getBadge(type: AddressType): String? {
    return when (type) {
        AddressType.NATIVE_SEGWIT -> stringResource(id = R.string.nc_wallet_default)
        AddressType.TAPROOT -> stringResource(id = R.string.nc_wallet_new)
        else -> null
    }
}

@Composable
private fun TypeOption(
    isViewOnly: Boolean = false,
    selected: Boolean = true,
    name: String,
    badge: String?,
    isEndItem: Boolean = false,
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .alpha(if ((isViewOnly && !selected) || disabled) 0.4f else 1f)
            .background(
                color = if (selected) MaterialTheme.colorScheme.fillDenim2 else MaterialTheme.colorScheme.surface,
                shape = if (isEndItem.not()) RoundedCornerShape(8.dp) else RoundedCornerShape(
                    topEnd = 8.dp,
                    topStart = 8.dp
                )
            )
            .padding(horizontal = 12.dp)
            .clickable(enabled = isViewOnly.not() && !disabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = name,
            style = NunchukTheme.typography.body
        )

        badge?.let {
            Text(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.whisper,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clip(shape = RoundedCornerShape(20.dp))
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                text = badge,
                style = NunchukTheme.typography.bold.copy(fontSize = 10.sp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        NcRadioButton(selected = selected, onClick = {
            if (!isViewOnly && !disabled) {
                onClick()
            }
        })
    }
}

@Composable
private fun InviteSection(
    invitations: List<GroupInvitation>,
    onAddInvite: () -> Unit,
    onRemoveInvite: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nc_wallet_invites),
                style = NunchukTheme.typography.titleSmall
            )
            NcIcon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onAddInvite),
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = stringResource(R.string.nc_wallet_add_invite),
            )
        }
        invitations.forEach { invitation ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invitation.recipientEmail,
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textSecondary
                )
                Text(
                    text = stringResource(R.string.nc_wallet_remove),
                    style = NunchukTheme.typography.titleSmall,
                    modifier = Modifier.clickable {
                        onRemoveInvite(invitation.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteEmailsBottomSheet(
    onDismiss: () -> Unit,
    onSendInvite: (List<String>) -> Unit,
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var emails by remember { mutableStateOf<List<InviteEmailChipState>>(emptyList()) }

    fun addEmails(rawInput: String): List<InviteEmailChipState> {
        val normalized = rawInput
            .replace("\n", " ")
            .split(",", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return emails
        var updatedEmails = emails
        normalized.forEach { email ->
            if (updatedEmails.none { it.email.equals(email, ignoreCase = true) }) {
                updatedEmails = updatedEmails + InviteEmailChipState(
                    email = email,
                    valid = EmailValidator.valid(email)
                )
            }
        }
        emails = updatedEmails
        return updatedEmails
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NcIcon(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onDismiss),
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = stringResource(R.string.nc_cancel),
                )
                Text(
                    text = stringResource(R.string.nc_wallet_send_invite),
                    style = NunchukTheme.typography.title.copy(
                        fontWeight = FontWeight.W700,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable {
                        var currentEmails = emails
                        if (inputText.isNotBlank()) {
                            currentEmails = addEmails(inputText)
                            inputText = ""
                        }
                        val validEmails = currentEmails
                            .filter { it.valid }
                            .map { it.email }
                        if (validEmails.isNotEmpty()) {
                            onSendInvite(validEmails)
                        }
                    },
                    color = MaterialTheme.colorScheme.textPrimary
                )
            }

            Text(
                text = stringResource(R.string.nc_wallet_invite_description),
                style = NunchukTheme.typography.body,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 144.dp, max = 480.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.border,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (emails.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emails.forEach { email ->
                            InviteEmailChip(
                                email = email,
                                onRemove = {
                                    emails = emails - email
                                }
                            )
                        }
                    }
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        inputText = newText
                        val lastChar = newText.lastOrNull()
                        if (lastChar == ',' || lastChar == ' ') {
                            addEmails(newText.dropLast(1))
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(top = if (emails.isEmpty()) 0.dp else 8.dp),
                    textStyle = NunchukTheme.typography.body,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputText.isNotBlank()) {
                                addEmails(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty() && emails.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.nc_wallet_invite_hint),
                                    style = NunchukTheme.typography.body.copy(
                                        color = MaterialTheme.colorScheme.textSecondary
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InviteEmailChip(
    email: InviteEmailChipState,
    onRemove: () -> Unit,
) {
    val backgroundColor = if (email.valid) Color(0xFFA7F0BA) else Color(0xFFFFD7D9)
    Surface(
        modifier = Modifier.clickable(onClick = onRemove),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = if (email.valid) painterResource(R.drawable.ic_check_circle_outline) else painterResource(
                    R.drawable.ic_cancel_red
                ),
                contentDescription = null,
                tint = Color(0xFF031F2B),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = email.email,
                style = NunchukTheme.typography.body,
                color = Color(0xFF031F2B)
            )
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = Color(0xFF031F2B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private data class InviteEmailChipState(
    val email: String,
    val valid: Boolean = true,
)

@Composable
fun KeyManagementSection(
    title: String,
    description: String,
    value: Int,
    enableIncrement: Boolean = true,
    enableDecrement: Boolean = true,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary,
                    fontWeight = FontWeight.W400
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .alpha(if (enableDecrement) 1f else 0.4f)
                    .border(1.dp, MaterialTheme.colorScheme.textPrimary, CircleShape)
                    .clickable(enableDecrement) {
                        onDecrement()
                    },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "Decrement",
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(48.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = NunchukTheme.typography.body,
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .alpha(if (enableIncrement) 1f else 0.4f)
                    .border(1.dp, MaterialTheme.colorScheme.textPrimary, CircleShape)
                    .clickable(enableIncrement) { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Increment",
                )
            }
        }
    }
}

@Composable
fun KeysAndRequiredKeysScreen(
    freeGroupWalletConfig: GlobalGroupWalletConfig,
    m: Int,
    n: Int,
    viewOnly: Boolean = false,
    onNumberChange: (Int, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .alpha(if (viewOnly) 0.4f else 1f)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(16.dp)
    ) {
        var keys by remember { mutableIntStateOf(n) }
        var requiredKeys by remember { mutableIntStateOf(m) }

        LaunchedEffect(keys, requiredKeys) {
            onNumberChange(requiredKeys, keys)
        }

        KeyManagementSection(
            title = "Keys",
            description = "Number of keys assigned to the wallet (up to ${freeGroupWalletConfig.maxKey}).",
            value = keys,
            enableIncrement = keys < freeGroupWalletConfig.maxKey && viewOnly.not(),
            enableDecrement = keys > 2 && keys > requiredKeys && viewOnly.not(),
            onIncrement = { if (keys < freeGroupWalletConfig.maxKey) keys++ },
            onDecrement = { if (keys > 2 && keys > requiredKeys) keys-- }
        )

        Spacer(modifier = Modifier.height(16.dp))

        KeyManagementSection(
            title = "Required keys",
            description = "Number of signatures required to unlock funds.",
            value = requiredKeys,
            enableIncrement = requiredKeys < keys && viewOnly.not(),
            enableDecrement = requiredKeys > 1 && viewOnly.not(),
            onIncrement = { if (requiredKeys < keys) requiredKeys++ },
            onDecrement = { if (requiredKeys > 1) requiredKeys-- }
        )
    }
}

@Preview
@Composable
private fun MiniscriptSection(onAddMiniscript: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFDEBD2),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Miniscript",
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.border,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "New",
                        style = NunchukTheme.typography.caption
                    )
                }
            }

            NcRadioButton(
                selected = true,
                onClick = { }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.border,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "No miniscript added yet. Start creating your own spending rules.",
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary
                )

                NcPrimaryDarkButton(
                    modifier = Modifier,
                    height = 36.dp,
                    onClick = onAddMiniscript
                ) {
                    Text(
                        text = "Add miniscript",
                        style = NunchukTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.controlTextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniscriptSectionFilled(
    miniscriptContent: String,
    onEdit: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.fillDenim2,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Miniscript",
                    style = NunchukTheme.typography.body
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.border,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "New",
                        style = NunchukTheme.typography.caption
                    )
                }
            }

            NcRadioButton(
                selected = true,
                onClick = { }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.border,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = miniscriptContent,
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable { onEdit() },
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NcIcon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(id = R.drawable.ic_edit_small),
                                contentDescription = "Edit"
                            )
                            Text(
                                text = "Edit",
                                style = NunchukTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.textPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.clickable { onRemove() },
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NcIcon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "Remove"
                            )
                            Text(
                                text = "Remove",
                                style = NunchukTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MiniscriptSectionFilledPreview() {
    NunchukTheme {
        MiniscriptSectionFilled(
            miniscriptContent = """andor(
  after(12960),
  thresh(3, pk(D), pk(E), pk(F)),
  thresh(3, pk(A), pk(B), pk(C))
)"""
        )
    }
}
