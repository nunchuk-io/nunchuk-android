package com.nunchuk.android.main.membership.onchaintimelock.checkfirmware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckFirmwareActivity : BaseComposeActivity() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val args: CheckFirmwareArgs by lazy {
        CheckFirmwareArgs.deserializeFrom(intent)
    }

    private val viewModel: CheckFirmwareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(args)

        enableEdgeToEdge()
        setContent {
            ColdCardCheckFirmwareScreen(
                args = args,
                viewModel = viewModel,
                onFilteredSignersReady = { signers ->
                    val resultIntent = Intent().apply {
                        putParcelableArrayListExtra(
                            GlobalResultKey.EXTRA_SIGNERS,
                            ArrayList(signers)
                        )
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
                onOpenNextScreen = ::openNextScreen
            )
        }
    }

    private fun openNextScreen() {
        when (args.signerTag) {
            SignerTag.COLDCARD -> {
                navigator.openSetupMk4(
                    activity = this,
                    args = SetupMk4Args(
                        fromMembershipFlow = args.onChainAddSignerParam != null,
                        isFromAddKey = true,
                        groupId = args.groupId,
                        walletId = args.walletId,
                        onChainAddSignerParam = args.onChainAddSignerParam,
                    )
                )
            }

            SignerTag.JADE -> {
                navigator.openAddAirSignerScreen(
                    activityContext = this,
                    args = AddAirSignerArgs(
                        isMembershipFlow = args.onChainAddSignerParam != null,
                        tag = SignerTag.JADE,
                        groupId = args.groupId,
                        walletId = args.walletId,
                        onChainAddSignerParam = args.onChainAddSignerParam,
                        step = membershipStepManager.currentStep
                    )
                )
            }

            else -> {
                // Handle other signer types if needed
            }
        }
        // Return result to SignerIntroFragment to handle navigation
        val resultIntent = Intent().apply {
            putExtra(EXTRA_OPEN_NEXT_SCREEN, true)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_OPEN_NEXT_SCREEN = "EXTRA_OPEN_NEXT_SCREEN"

        fun navigate(
            context: Context,
            launcher: ActivityResultLauncher<Intent>?,
            args: CheckFirmwareArgs
        ) {
            val intent = Intent(context, CheckFirmwareActivity::class.java).apply {
                putExtras(args.buildBundle())
            }
            if (launcher == null) {
                context.startActivity(intent)
            } else {
                launcher.launch(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColdCardCheckFirmwareScreen(
    args: CheckFirmwareArgs,
    viewModel: CheckFirmwareViewModel = viewModel(),
    onMoreClicked: () -> Unit = {},
    onFilteredSignersReady: (List<SignerModel>) -> Unit = {},
    onOpenNextScreen: () -> Unit = {}
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val firmwareVersion by viewModel.firmwareVersion.collectAsStateWithLifecycle()
    val filteredSigners by viewModel.filteredSigners.collectAsStateWithLifecycle()
    var showSignerBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CheckFirmwareEvent.ShowFilteredSigners -> {
                    if (args.onChainAddSignerParam?.isClaiming == true) {
                        showSignerBottomSheet = true
                    } else {
                        onFilteredSignersReady(event.signers)
                    }
                }

                CheckFirmwareEvent.OpenNextScreen -> {
                    onOpenNextScreen()
                }
            }
        }
    }

    ColdCardCheckFirmwareContent(
        args = args,
        onMoreClicked = onMoreClicked,
        remainTime = remainTime,
        firmwareVersion = firmwareVersion,
        onContinueClicked = viewModel::onContinueClicked
    )

    if (showSignerBottomSheet && filteredSigners.isNotEmpty()) {
        NunchukTheme {
            SelectSignerBottomSheet(
                sheetState = bottomSheetState,
                onDismiss = {
                    showSignerBottomSheet = false
                },
                onAddExistKey = { signer ->
                    showSignerBottomSheet = false
                    onFilteredSignersReady(listOf(signer))
                },
                onAddNewKey = {
                    showSignerBottomSheet = false
                    onOpenNextScreen()
                },
                args = TapSignerListBottomSheetFragmentArgs(
                    signers = filteredSigners.toTypedArray(),
                    type = when (args.signerTag) {
                        SignerTag.COLDCARD -> SignerType.COLDCARD_NFC
                        SignerTag.JADE -> SignerType.AIRGAP
                        else -> SignerType.UNKNOWN
                    }
                )
            )
        }
    }
}

@Composable
private fun ColdCardCheckFirmwareContent(
    args: CheckFirmwareArgs,
    remainTime: Int = 0,
    firmwareVersion: String = "",
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    val signerName = when (args.signerTag) {
        SignerTag.COLDCARD -> "COLDCARD"
        SignerTag.JADE -> "Jade"
        else -> args.signerTag.name
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = when (args.signerTag) {
                        SignerTag.JADE -> R.drawable.bg_add_jade
                        else -> R.drawable.bg_check_coldcard_firmware_illustration
                    },
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Check your $signerName firmware",
                    style = NunchukTheme.typography.heading
                )
                val context = LocalContext.current
                val annotatedText = buildAnnotatedString {
                    append(
                        "Your $signerName must support Miniscript for on-chain timelocks. Please make sure the device is running "
                    )
                    if (firmwareVersion.isNotEmpty()) {
                        append(firmwareVersion)
                        append(" or above.")
                        append(" ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                            append("$firmwareVersion or above.")
                        }
                    } else {
                        append("the latest firmware version.")
                    }
                    append("\n\n")
                    append("To check or update $signerName firmware, please follow ")
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = when (args.signerTag) {
                            SignerTag.COLDCARD -> "https://coldcard.com/docs/upgrade/"
                            SignerTag.JADE -> "https://help.blockstream.com/hc/en-us/articles/4403295799577-How-do-I-update-the-firmware-on-my-Blockstream-Jade"
                            else -> ""
                        }
                    )
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF1976D2),
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("$signerName's instructions")
                    }
                    pop()
                    append(".")
                }

                ClickableText(
                    modifier = Modifier.padding(16.dp),
                    text = annotatedText,
                    style = NunchukTheme.typography.body,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ColdCardCheckFirmwareScreenPreview() {
    ColdCardCheckFirmwareContent(
        args = CheckFirmwareArgs(
            signerTag = SignerTag.COLDCARD
        ),
    )
}

