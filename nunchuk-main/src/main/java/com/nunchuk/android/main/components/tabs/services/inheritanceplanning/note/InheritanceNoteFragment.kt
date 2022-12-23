package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritanceNoteFragment : MembershipFragment() {

    private val viewModel: InheritanceNoteViewModel by viewModels()
    private val args: InheritanceNoteFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritanceNoteScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceNoteEvent.ContinueClick -> {
                    if (args.isUpdateRequest || args.planFlow == InheritancePlanFlow.VIEW) {
                        setFragmentResult(
                            REQUEST_KEY, bundleOf(EXTRA_NOTE to event.note)
                        )
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(
                            InheritanceNoteFragmentDirections.actionInheritanceNoteFragmentToInheritanceNotifyPrefFragment(
                                activationDate = args.activationDate,
                                verifyToken = args.verifyToken,
                                note = event.note,
                                magicalPhrase = args.magicalPhrase,
                                planFlow = args.planFlow
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "InheritanceNoteFragment"
        const val EXTRA_NOTE = "EXTRA_NOTE"
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun InheritanceNoteScreen(
    viewModel: InheritanceNoteViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()

    InheritanceNoteScreenContent(
        remainTime = remainTime,
        note = state.note,
        onContinueClick = viewModel::onContinueClicked,
        onTextChange = viewModel::updateNote
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InheritanceNoteScreenContent(
    remainTime: Int = 0,
    note: String = "",
    onContinueClick: () -> Unit = {},
    onTextChange: (value: String) -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_leave_message),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_inheritance_leave_message_desc),
                    style = NunchukTheme.typography.body
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp),
                        text = stringResource(id = R.string.nc_note),
                        style = NunchukTheme.typography.titleSmall
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        text = stringResource(id = R.string.nc_optional),
                        style = NunchukTheme.typography.bodySmall
                    )
                }

                val interactionSource: MutableInteractionSource =
                    remember { MutableInteractionSource() }

                BasicTextField(value = note,
                    onValueChange = onTextChange,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colors.surface, shape = RoundedCornerShape(8.dp)
                        )
                        .height(145.dp)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    decorationBox = @Composable { innerTextField ->
                        TextFieldDefaults.OutlinedTextFieldDecorationBox(value = note,
                            visualTransformation = VisualTransformation.None,
                            label = null,
                            innerTextField = innerTextField,
                            leadingIcon = null,
                            trailingIcon = null,
                            enabled = true,
                            isError = false,
                            singleLine = false,
                            interactionSource = interactionSource,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            border = {
                                Box(
                                    Modifier.border(
                                        width = 1.dp,
                                        color = Color(0xFFDEDEDE),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                )
                            })
                    })

                Spacer(modifier = Modifier.weight(1.0f))

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_update_message))
                }
            }
        }
    }
}

@Preview
@Composable
private fun InheritanceNoteScreenPreview() {
    InheritanceNoteScreenContent()
}