package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.utils.simpleGlobalDateFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class InheritanceActivationDateFragment : MembershipFragment() {

    private val viewModel: InheritanceActivationDateViewModel by viewModels()
    private val args: InheritanceActivationDateFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritanceActivationDateScreen(viewModel,
                    args,
                    onDatePicker = {
                        showDatePicker()
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceActivationDateEvent.ContinueClick -> {
                    if (args.isUpdateRequest || args.planFlow == InheritancePlanFlow.VIEW) {
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(EXTRA_ACTIVATION_DATE to event.date)
                        )
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(
                            InheritanceActivationDateFragmentDirections.actionInheritanceActivationDateFragmentToInheritanceNoteFragment(
                                activationDate = event.date,
                                verifyToken = args.verifyToken,
                                magicalPhrase = args.magicalPhrase,
                                planFlow = args.planFlow
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(requireContext(), R.style.NunchukDateTimePicker)
        dialog.setOnDateSetListener { _, year, month, dayOfMonth ->
            viewModel.setDate(year, month, dayOfMonth)
        }
        dialog.show()
    }

    companion object {
        const val REQUEST_KEY = "InheritanceActivationDateFragment"
        const val EXTRA_ACTIVATION_DATE = "EXTRA_ACTIVATION_DATE"
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun InheritanceActivationDateScreen(
    viewModel: InheritanceActivationDateViewModel = viewModel(),
    args: InheritanceActivationDateFragmentArgs,
    onDatePicker: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    val date = if (state.date > 0) Date(state.date).simpleGlobalDateFormat() else ""

    InheritanceActivationDateScreenContent(
        remainTime = remainTime,
        date = date,
        planFlow = args.planFlow,
        isUpdateRequest = args.isUpdateRequest,
        onContinueClick = {
            viewModel.onContinueClicked()
        }, onDatePick = {
            onDatePicker()
        })
}

@Composable
fun InheritanceActivationDateScreenContent(
    remainTime: Int = 0,
    date: String = "",
    planFlow: Int = InheritancePlanFlow.NONE,
    isUpdateRequest: Boolean = false,
    onContinueClick: () -> Unit = {},
    onDatePick: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val isSetupFlow = planFlow == InheritancePlanFlow.SETUP && isUpdateRequest.not()
                val title = if (isSetupFlow) stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ) else ""
                NcTopAppBar(title = title)
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_set_up_activation_date),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_set_up_activation_date_desc),
                    style = NunchukTheme.typography.body
                )

                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    val (title, input) = createRefs()
                    Text(text = stringResource(id = R.string.nc_activation_date),
                        style = NunchukTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .constrainAs(title) {})
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = NcColor.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = { onDatePick() })
                            .constrainAs(input) {
                                top.linkTo(title.bottom)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val textStyle =
                            if (date.isEmpty()) NunchukTheme.typography.body.copy(
                                color = NcColor.boulder
                            ) else NunchukTheme.typography.body
                        Text(
                            text = date
                                .ifBlank { stringResource(id = R.string.nc_activation_date_holder) },
                            style = textStyle,
                            modifier = Modifier
                                .padding(top = 14.dp, start = 12.dp, bottom = 14.dp)
                                .weight(1f)
                                .defaultMinSize(minWidth = TextFieldDefaults.MinWidth)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))

                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_set_up_activation_date_notice)))
                )
                val continueBtnText =
                    if (isSetupFlow) stringResource(id = R.string.nc_text_continue) else stringResource(
                        id = R.string.nc_update_activation_date
                    )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick,
                    enabled = date.isNotBlank()
                ) {
                    Text(text = continueBtnText)
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceActivationDateScreenPreview() {
    InheritanceActivationDateScreenContent()
}