package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate.InheritanceActivationDateViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanFragmentDirections
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanViewModel

class InheritanceNoteFragment : Fragment() {

    private val viewModel: InheritanceNoteViewModel by viewModels()

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
        flowObserver(viewModel.event) {
            when (it) {
                InheritanceNoteEvent.ContinueClick -> findNavController().navigate(
                    InheritanceNoteFragmentDirections.actionInheritanceNoteFragmentToInheritanceNotifyPrefFragment()
                )
            }
        }
    }
}

@Composable
fun InheritanceNoteScreen(
    viewModel: InheritanceNoteViewModel = viewModel()
) {
    InheritanceNoteScreenContent(onContinueClick = {
        viewModel.onContinueClicked()
    })
}

@Composable
fun InheritanceNoteScreenContent(
    date: String = "",
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
                NcTopAppBar(title = "")
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

                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    title = stringResource(id = R.string.nc_note),
                    value = date,
                    onValueChange = onTextChange
                )
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