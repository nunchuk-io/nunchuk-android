package com.nunchuk.android.app.referral.invitefriend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.app.referral.ConfirmationCodeResultData
import com.nunchuk.android.app.referral.ReferralAction
import com.nunchuk.android.app.referral.address.REFERRAL_ADDRESS_RESULT
import com.nunchuk.android.app.referral.confirmationcode.REFERRAL_CONFIRMATION_CODE_RESULT
import com.nunchuk.android.app.referral.simplifyAddress
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.html.HtmlText
import com.nunchuk.android.core.referral.ReferralArgs
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.model.campaigns.CampaignStatus
import com.nunchuk.android.model.campaigns.CampaignType
import com.nunchuk.android.signer.R
import com.nunchuk.android.utils.EmailValidator
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

const val referralInviteFriendRoute = "referral_invite_friend_route"

fun NavGraphBuilder.referralInviteFriend(
    navController: NavController,
    args: ReferralArgs,
    snackState: SnackbarHostState,
    onCopyToClipboard: (String) -> Unit,
    onChangeAddress: (String, String, Boolean) -> Unit,
    onViewReferralAddress: (String) -> Unit = {},
    onShareLink: (Boolean, String) -> Unit = { _, _ -> },
) {
    composable(referralInviteFriendRoute) {

        val viewModel = hiltViewModel<ReferralInviteFriendViewModel>()
        viewModel.init(args.campaign, args.localReferrerCode)
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.errorMsg) {
            state.errorMsg?.let {
                snackState.showSnackbar(
                    NcSnackbarVisuals(
                        type = NcToastType.ERROR,
                        message = it,
                    )
                )
                viewModel.onErrorMessageEventConsumed()
            }
        }

        LaunchedEffect(state.event) {
            state.event?.let {
                when (it) {
                    ReferralInviteFriendEvent.ChangeAddressSuccess -> {
                        snackState.showSnackbar(
                            NcSnackbarVisuals(
                                type = NcToastType.SUCCESS,
                                message = "Address change successful.",
                            )
                        )
                    }
                }
                viewModel.onEventConsumed()
            }
        }

        ReferralInviteFriendScreen(
            args = args,
            state = state,
            navController = navController,
            snackState = snackState,
            isPickTempAddress = viewModel.isPickTempAddress(),
            onGenerateReferralLink = {
                viewModel.createReferrerCodeByEmail(it)
            },
            onShowEmailAlreadyExistDialogConsumed = { isYesClick, inputEmail ->
                viewModel.onShowEmailAlreadyExistDialogConsumed(isYesClick, inputEmail)
            },
            onForceShowInputEmail = {
                viewModel.setForceShowInputEmail(it)
            },
            onCopyToClipboard = {
                state.localReferrerCode?.link?.let { link ->
                    onCopyToClipboard(link)
                }
            },
            onChangeAddress = {
                onChangeAddress(
                    viewModel.getReceiveAddress(),
                    viewModel.getSelectWalletId(),
                    viewModel.isHasLocalReferrerCode()
                )
            },
            onViewReferralAddress = {
                if (viewModel.getEmail().isNotEmpty()) {
                    onViewReferralAddress(viewModel.getEmail())
                }
            },
            onShowReceiveAddressResult = {
                viewModel.getReferrerCodeByEmail(viewModel.getEmail(), it)
            },
            onChangeReceiveAddress = {
                viewModel.updateReceiveAddress(resultData = it)
            },
            onShareLink = onShareLink,
            onPickReceiveAddressResult = {
                viewModel.updatePickTempAddress(it)
            },
            onShowNoInternetDialogConsumed = {
                viewModel.onShowNoInternetDialogConsumed()
            },
            onDismissCampaign = {
                viewModel.dismissCampaign()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralInviteFriendScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = SnackbarHostState(),
    navController: NavController = NavController(LocalContext.current),
    args: ReferralArgs,
    state: ReferralInviteFriendUiState,
    isPickTempAddress: Boolean = false,
    onGenerateReferralLink: (String) -> Unit = {},
    onShowEmailAlreadyExistDialogConsumed: (Boolean, String) -> Unit = { _, _ -> },
    onShowNoInternetDialogConsumed: () -> Unit = { },
    onForceShowInputEmail: (Boolean) -> Unit = {},
    onCopyToClipboard: () -> Unit = {},
    onChangeAddress: () -> Unit = {},
    onViewReferralAddress: () -> Unit = {},
    onShowReceiveAddressResult: (ConfirmationCodeResultData) -> Unit = {},
    onChangeReceiveAddress: (ConfirmationCodeResultData) -> Unit = {},
    onPickReceiveAddressResult: (ConfirmationCodeResultData) -> Unit = {},
    onShareLink: (Boolean, String) -> Unit = { _, _ -> },
    onDismissCampaign: () -> Unit = {},
) {
    var email by remember { mutableStateOf(state.localReferrerCode?.email.orEmpty()) }
    var showDismissCampaignSheet by remember { mutableStateOf(false) }
    var showDismissCampaignDialog by remember { mutableStateOf(false) }
    val isDownloadCampaignType = args.campaign.type == CampaignType.DOWNLOAD

    val context = LocalLifecycleOwner.current

    val localContext = LocalContext.current

    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<ConfirmationCodeResultData>(
            REFERRAL_CONFIRMATION_CODE_RESULT
        )?.distinctUntilChanged()
            ?.observe(context) { result ->
                if (result.action == ReferralAction.VIEW.value) {
                    navController.currentBackStackEntry?.savedStateHandle?.remove<ConfirmationCodeResultData>(
                        REFERRAL_CONFIRMATION_CODE_RESULT
                    )
                    onShowReceiveAddressResult(result)
                }
            }
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<ConfirmationCodeResultData>(
            REFERRAL_ADDRESS_RESULT
        )?.distinctUntilChanged()
            ?.observe(context) { result ->
                if (result.action == ReferralAction.CHANGE.value) {
                    onChangeReceiveAddress(result)
                } else if (result.action == ReferralAction.PICK.value) {
                    onPickReceiveAddressResult(result)
                }
                navController.currentBackStackEntry?.savedStateHandle?.remove<ConfirmationCodeResultData>(
                    REFERRAL_ADDRESS_RESULT
                )
            }
    }

    NcScaffold(
        modifier = modifier.systemBarsPadding(),
        snackState = snackState,
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_invite_friends),
                isBack = false,
                textStyle = NunchukTheme.typography.titleLarge,
                actions = {
                    if (state.campaign?.dismissible == true && state.campaign.isDismissed.not()) {
                        IconButton(onClick = {
                            showDismissCampaignSheet = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                }
            )
        },
        bottomBar = {
            if ((state.localReferrerCode == null && state.isLoginByEmail.not()) || state.forceShowInputEmail) {
                val enableButton = if (isPickTempAddress) {
                    EmailValidator.valid(email) && email != state.localReferrerCode?.email
                } else {
                    EmailValidator.valid(email)
                            && email != state.localReferrerCode?.email
                            && state.isHideAddress().not()
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    enabled = enableButton,
                    onClick = {
                        onGenerateReferralLink(email)
                    },
                ) {
                    Text(text = stringResource(R.string.nc_generate_referral_link))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            GlideImage(modifier = Modifier
                .height(100.dp)
                .padding(top = 12.dp),
                imageModel = {
                    args.campaign.referrerBannerUrl
                }, imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop, alignment = Alignment.Center
                ), loading = {

                }, failure = {
                })
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = args.campaign.referrerTitle,
                style = NunchukTheme.typography.heading,
            )

            HtmlText(
                modifier = Modifier.padding(top = 16.dp),
                text = args.campaign.referrerDescriptionHtml,
                style = NunchukTheme.typography.body,
                linkClicked = { link ->
                    localContext.openExternalLink(link)
                }
            )

            if (isDownloadCampaignType.not()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nc_receive_reward_via),
                        style = NunchukTheme.typography.title,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                onChangeAddress()
                            }
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clickable {
                                onChangeAddress()
                            },
                        text = "Change",
                        style = NunchukTheme.typography.captionTitle
                    )
                }

                if (isPickTempAddress.not() && state.isHideAddress()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "●●●●●●",
                            style = NunchukTheme.typography.body,
                            fontSize = 20.sp
                        )

                        Image(
                            painter = painterResource(id = R.drawable.ic_eye),
                            contentDescription = "",
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp, top = 2.dp)
                                .clickable {
                                    onViewReferralAddress()
                                }
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.border,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Column {
                            if (isPickTempAddress) {
                                Text(
                                    text = state.receiveWalletTemp?.wallet?.name.orEmpty(),
                                    style = NunchukTheme.typography.title
                                )
                                Text(
                                    text = simplifyAddress(state.receiveWalletTemp?.receiveAddress.orEmpty()),
                                    style = NunchukTheme.typography.body
                                )
                            } else {
                                if (state.wallet?.name.isNullOrEmpty().not()) {
                                    Text(
                                        text = state.wallet?.name.orEmpty(),
                                        style = NunchukTheme.typography.title
                                    )
                                }
                                Text(
                                    text = state.getDisplayAddress(),
                                    style = NunchukTheme.typography.body
                                )
                            }
                        }
                    }
                }
            }

            if ((state.isLoginByEmail.not() && state.localReferrerCode == null) || state.forceShowInputEmail) {
                NcTextField(
                    modifier = Modifier.padding(top = 20.dp),
                    title = stringResource(R.string.nc_your_email),
                    value = email,
                    onValueChange = {
                        email = it
                    })
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.nc_we_send_email_confirm_reward_address),
                    style = NunchukTheme.typography.bodySmall,
                )
            }

            if (state.isLoginByEmail.not() && state.localReferrerCode != null && state.forceShowInputEmail.not()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nc_referrer_email),
                        style = NunchukTheme.typography.title,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                onForceShowInputEmail(true)
                            }
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clickable {
                                onForceShowInputEmail(true)
                            },
                        text = stringResource(R.string.nc_change),
                        style = NunchukTheme.typography.captionTitle
                    )
                }
                Text(text = state.localReferrerCode.email, style = NunchukTheme.typography.body)
            }

            if (isDownloadCampaignType && state.localReferrerCode != null && state.forceShowInputEmail.not()
                && state.localReferrerCode.status != CampaignStatus.COMPLETED
            ) {
                Row(modifier = Modifier.padding(top = if (state.isLoginByEmail) 8.dp else 16.dp)) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.nc_friend_downloaded),
                        style = NunchukTheme.typography.title,
                    )
                    Text(
                        text = state.localReferrerCode.getDisplayDownloaded(),
                        style = NunchukTheme.typography.body,
                    )
                }
            }

            if (state.localReferrerCode?.link.isNullOrEmpty()
                    .not() && state.forceShowInputEmail.not()
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

                Text(text = "Share your link", style = NunchukTheme.typography.title)

                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            text = state.getDisplayLink(),
                            style = NunchukTheme.typography.body,
                            maxLines = 1,
                        )
                    }

                    NcPrimaryDarkButton(
                        onClick = {
                            onCopyToClipboard()
                        },
                    ) {
                        Text(text = stringResource(R.string.nc_copy))
                    }
                }
                Text(
                    text = "More ways to share",
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nc_ic_share_message),
                        contentDescription = "",
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                onShareLink(true, state.localReferrerCode?.textTemplate.orEmpty())
                            }
                    )
                    Image(
                        painter = painterResource(id = R.drawable.nc_ic_share),
                        contentDescription = "",
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                onShareLink(false, state.localReferrerCode?.textTemplate.orEmpty())
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (state.showEmailAlreadyExist) {
            NcInfoDialog(
                title = stringResource(id = R.string.nc_confirmation),
                message = stringResource(R.string.nc_email_address_already_associated_referral_link),
                positiveButtonText = stringResource(R.string.nc_text_yes),
                negativeButtonText = stringResource(R.string.nc_use_another_email),
                isOutlineButton = true,
                onNegativeClick = {
                    onShowEmailAlreadyExistDialogConsumed(false, email)
                },
                onPositiveClick = {
                    onForceShowInputEmail(false)
                    onShowEmailAlreadyExistDialogConsumed(true, email)
                },
                onDismiss = {
                    onShowEmailAlreadyExistDialogConsumed(false, email)
                },
            )
        }
        if (state.showNoInternet) {
            NcInfoDialog(
                title = stringResource(id = R.string.nc_text_warning),
                message = stringResource(R.string.nc_no_internet_connection_try_again_later),
                onDismiss = {
                    onShowNoInternetDialogConsumed()
                },
            )
        }

        if (showDismissCampaignDialog) {
            NcInfoDialog(
                title = stringResource(id = R.string.nc_text_info),
                message = stringResource(R.string.nc_dismiss_referral_from_home_page),
                onDismiss = {
                    showDismissCampaignDialog = false
                    onDismissCampaign()
                },
            )
        }

        if (showDismissCampaignSheet) {
            NcSelectableBottomSheet(
                options = listOf(stringResource(R.string.nc_dismiss_from_home_screen)),
                onSelected = {
                    showDismissCampaignDialog = true
                    showDismissCampaignSheet = false
                },
                onDismiss = { showDismissCampaignSheet = false },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReferralInviteFriendScreenPreview() {
    NunchukTheme {
        ReferralInviteFriendScreen(
            args = ReferralArgs.default,
            state = ReferralInviteFriendUiState(isLoginByEmail = false),
        )
    }
}