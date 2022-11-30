package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R

class InheritanceReviewPlanFragment : Fragment() {

    private val viewModel: InheritanceReviewPlanViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                InheritanceReviewPlanScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                InheritanceReviewPlanEvent.ContinueClick -> findNavController().navigate(
                    InheritanceReviewPlanFragmentDirections.actionInheritanceReviewPlanFragmentToInheritanceActivationDateFragment()
                )
            }
        }
    }
}

@Composable
fun InheritanceReviewPlanScreen(
    viewModel: InheritanceReviewPlanViewModel = viewModel()
) {
    InheritanceReviewPlanScreenContent(onContinueClicked = {
        viewModel.onContinueClicked()
    })
}

@Composable
fun InheritanceReviewPlanScreenContent(
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
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

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(top = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                item {

                                    Text(
                                        text = stringResource(id = R.string.nc_review_your_plan),
                                        style = NunchukTheme.typography.heading
                                    )
                                }
                                item {
                                    Box(
                                        modifier = Modifier.background(
                                            color = NcColor.denimTint,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_assisted_wallet_intro),
                                                contentDescription = null
                                            )
                                            Column(
                                                modifier = Modifier
                                                    .weight(1.0f)
                                                    .padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    modifier = Modifier.padding(top = 4.dp),
                                                    text = "The beneficiary will inherit",
                                                    style = NunchukTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = colorResource(id = R.color.nc_denim_tint_color),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column() {
                                            Text(
                                                text = stringResource(id = R.string.nc_inheritance_review_plan_note),
                                                style = NunchukTheme.typography.title
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            DetailPlanItem(
                                                iconId = R.drawable.ic_nc_calendar,
                                                titleId = R.string.nc_activation_date,
                                                content = "[MM/DD/YYYY]"
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            DetailPlanItem(
                                                iconId = R.drawable.ic_nc_star_dark,
                                                titleId = R.string.nc_magical_phrase,
                                                content = "dolphin concert apple"
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            DetailPlanItem(
                                                iconId = R.drawable.ic_password,
                                                titleId = R.string.nc_backup_password,
                                                content = "Printed on the back of the inheritance key"
                                            )
                                        }
                                    }
                                }

                                item {
                                    Column(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp
                                        )
                                    ) {
                                        Row(horizontalArrangement = Arrangement.Center) {
                                            Text(
                                                text = stringResource(id = R.string.nc_note_to_beneficiary_trustee),
                                                style = NunchukTheme.typography.title
                                            )
                                            Spacer(modifier = Modifier.weight(weight = 1f))
                                            Text(
                                                text = stringResource(id = R.string.nc_edit),
                                                style = NunchukTheme.typography.title
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Box(
                                            modifier = Modifier.background(
                                                color = NcColor.greyLight,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "(No note)",
                                                style = NunchukTheme.typography.body,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    }
                                }

                                item {
                                    Divider(
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                        thickness = 1.dp,
                                        color = NcColor.whisper
                                    )
                                }

                                item {
                                    Column(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp
                                        )
                                    ) {
                                        Row(horizontalArrangement = Arrangement.Center) {
                                            Text(
                                                text = stringResource(id = R.string.nc_notification_preferences),
                                                style = NunchukTheme.typography.title,
                                            )
                                            Spacer(modifier = Modifier.weight(weight = 1f))
                                            Text(
                                                text = stringResource(id = R.string.nc_edit),
                                                style = NunchukTheme.typography.title,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = NcColor.greyLight,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = stringResource(id = R.string.nc_beneficiary_trustee_email_address),
                                                        style = NunchukTheme.typography.body,
                                                        modifier = Modifier.fillMaxWidth(0.3f)
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "(None listed)",
                                                        style = NunchukTheme.typography.title
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            NcPrimaryDarkButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                onContinueClicked
                            ) {
                                Text(text = stringResource(id = R.string.nc_text_continue))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPlanItem(
    iconId: Int = R.drawable.ic_nc_star_dark,
    titleId: Int = R.string.nc_text_continue,
    content: String = "dolphin concert apple"
) {
    Column {
        Row(horizontalArrangement = Arrangement.Center) {
            Icon(painter = painterResource(id = iconId), contentDescription = "")
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(id = titleId),
                style = NunchukTheme.typography.title
            )
            Spacer(modifier = Modifier.weight(weight = 1f))
            Text(
                text = stringResource(id = R.string.nc_edit),
                style = NunchukTheme.typography.title,
                textDecoration = TextDecoration.Underline
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.background(
                color = NcColor.greyLight,
                shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = content,
                style = NunchukTheme.typography.body,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun DetailPlanItemPreview() {
    DetailPlanItem()
}


@Preview
@Composable
private fun InheritanceReviewPlanScreenPreview() {
    InheritanceReviewPlanScreenContent()
}