package com.nunchuk.android.main.membership.key

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddKeyStepFragment : Fragment(), BottomSheetOptionListener {
    private val viewModel by viewModels<AddKeyStepViewModel>()

    @Inject
    lateinit var nunchukNavigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AddKeyStepScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AddKeyStepEvent.OpenContactUs -> requireActivity().sendEmail(event.email)
                AddKeyStepEvent.OpenAddKeyList -> handleOpenKeyList()
                AddKeyStepEvent.OpenRecoveryQuestion -> handleOpenRecoveryQuestion()
                AddKeyStepEvent.OpenCreateWallet -> handleOpenCreateWallet()
                AddKeyStepEvent.OnMoreClicked -> handleShowMore()
                AddKeyStepEvent.RestartWizardSuccess -> requireActivity().finish()
                AddKeyStepEvent.OpenInheritanceSetup -> handleOpenInheritanceSetup()
            }
        }
    }

    private fun handleOpenInheritanceSetup() {
        nunchukNavigator.openInheritancePlanningScreen(requireContext(), flowInfo = InheritancePlanFlow.SETUP)
    }

    private fun handleShowMore() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_RESTART_WIZARD,
                    label = getString(R.string.nc_restart_wizard)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXIT_WIZARD,
                    label = getString(R.string.nc_exit_wizard)
                )
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_RESTART_WIZARD) {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_confirm_restart_wizard),
                onYesClick = {
                    viewModel.resetWizard()
                }
            )
        } else if (option.type == SheetOptionType.TYPE_EXIT_WIZARD) {
            NCInfoDialog(requireActivity()).showDialog(
                message = getString(R.string.nc_resume_wizard_desc),
                onYesClick = {
                    requireActivity().finish()
                }
            )
        }
    }

    private fun handleOpenCreateWallet() {
        findNavController().navigate(AddKeyStepFragmentDirections.actionAddKeyStepFragmentToCreateWalletFragment())
    }

    private fun handleOpenKeyList() {
        findNavController().navigate(AddKeyStepFragmentDirections.actionAddKeyStepFragmentToAddKeyListFragment())
    }

    private fun handleOpenRecoveryQuestion() {
        findNavController().navigate(AddKeyStepFragmentDirections.actionAddKeyListFragmentToRecoveryQuestionFragment())
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AddKeyStepScreen(viewModel: AddKeyStepViewModel = viewModel()) {
    val isConfigKeyDone by viewModel.isConfigKeyDone.collectAsStateWithLifecycle()
    val isSetupRecoverKeyDone by viewModel.isSetupRecoverKeyDone.collectAsStateWithLifecycle()
    val isCreateWalletDone by viewModel.isCreateWalletDone.collectAsStateWithLifecycle()
    val groupRemainTime by viewModel.groupRemainTime.collectAsStateWithLifecycle()

    AddKeyStepContent(
        isConfigKeyDone = isConfigKeyDone,
        isSetupRecoverKeyDone = isSetupRecoverKeyDone,
        isCreateWalletDone = isCreateWalletDone,
        groupRemainTime = groupRemainTime,
        onMoreClicked = viewModel::onMoreClicked,
        onContinueClicked = viewModel::onContinueClicked,
        openContactUs = viewModel::openContactUs,
        plan = viewModel.plan
    )
}

@Composable
fun AddKeyStepContent(
    isConfigKeyDone: Boolean = false,
    isSetupRecoverKeyDone: Boolean = false,
    isCreateWalletDone: Boolean = false,
    groupRemainTime: IntArray = IntArray(4),
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    openContactUs: (mail: String) -> Unit = {},
    plan: MembershipPlan = MembershipPlan.HONEY_BADGER,
) = NunchukTheme {
    val imageBannerId =
        when {
            isCreateWalletDone -> R.drawable.bg_inheritance
            isSetupRecoverKeyDone -> R.drawable.bg_create_a_wallet
            isConfigKeyDone -> R.drawable.bg_setup_recovery_key
            else -> R.drawable.nc_bg_let_s_add_keys
        }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            NcImageAppBar(backgroundRes = imageBannerId, actions = {
                IconButton(onClick = onMoreClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more),
                        contentDescription = "More icon"
                    )
                }
            })
            StepWithEstTime(
                1,
                stringResource(id = R.string.nc_add_your_keys),
                groupRemainTime[0],
                isConfigKeyDone,
                isConfigKeyDone.not()
            )
            if (isConfigKeyDone.not()) {
                NcHintMessage(
                    modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp),
                    messages = listOf(ClickAbleText("This step requires hardware keys to complete. If you have not received your hardware after a while, please contact us at"),
                        ClickAbleText(CONTACT_EMAIL) {
                            openContactUs(CONTACT_EMAIL)
                        })
                )
            }
            StepWithEstTime(
                2,
                stringResource(R.string.nc_setup_key_recovery),
                groupRemainTime[1],
                isSetupRecoverKeyDone,
                isConfigKeyDone && isSetupRecoverKeyDone.not()
            )
            StepWithEstTime(
                3,
                stringResource(R.string.nc_create_your_wallet),
                groupRemainTime[2],
                isCreateWalletDone,
                isSetupRecoverKeyDone && isCreateWalletDone.not()
            )
            if (plan == MembershipPlan.HONEY_BADGER) {
                StepWithEstTime(
                    4,
                    stringResource(R.string.nc_set_up_inheritance_plan),
                    groupRemainTime[3],
                    false,
                    isCreateWalletDone
                )
            }
            Spacer(modifier = Modifier.weight(1.0f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked
            ) {
                Text(text = if (isConfigKeyDone.not()) stringResource(R.string.nc_start) else stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@Composable
fun StepWithEstTime(
    index: Int,
    label: String,
    estInMinutes: Int,
    isCompleted: Boolean,
    isInProgress: Boolean
) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
        text = "${stringResource(R.string.nc_step)} $index",
        style = NunchukTheme.typography.titleSmall
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = NunchukTheme.typography.body)
        if (isCompleted) {
            Text(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = colorResource(id = R.color.nc_whisper_color),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = stringResource(R.string.nc_text_completed),
                style = NunchukTheme.typography.caption
            )
        } else {
            val modifier = if (isInProgress) Modifier
                .background(
                    color = colorResource(id = R.color.nc_green_color),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
            else Modifier
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.nc_whisper_color),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
            Text(
                modifier = modifier,
                text = stringResource(R.string.nc_est_time_in_mins, estInMinutes),
                style = NunchukTheme.typography.caption
            )
        }
    }
}

@Preview
@Composable
fun AddKeyStepScreenPreview() {
    AddKeyStepContent(isSetupRecoverKeyDone = false, isConfigKeyDone = false, isCreateWalletDone = true)
}