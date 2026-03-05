package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.serialization.Serializable

@Serializable
data object MagicalPhraseIntroRoute

fun NavGraphBuilder.magicalPhraseIntro(
    onContinueClicked: (magicalPhrase: String, inheritanceKeys: List<String>) -> Unit,
) {
    composable<MagicalPhraseIntroRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val viewModel = hiltViewModel<MagicalPhraseIntroViewModel>()
        val sharedState by activityViewModel.state.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(Unit) {
            viewModel.init(activityViewModel.setupOrReviewParam)
        }

        LaunchedEffect(viewModel, lifecycleOwner) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is MagicalPhraseIntroEvent.OnContinueClicked -> {
                            onContinueClicked(event.magicalPhrase, event.inheritanceKeys)
                        }
                        is MagicalPhraseIntroEvent.Error -> NCToastMessage(activity).showError(event.message)
                        is MagicalPhraseIntroEvent.Loading -> activity.showOrHideLoading(event.loading)
                    }
                }
        }
        MagicalPhraseIntroScreen(
            viewModel = viewModel,
            isMiniscriptWallet = sharedState.isMiniscriptWallet,
        )
    }
}

fun NavController.navigateToMagicalPhraseIntro() { navigate(MagicalPhraseIntroRoute) }
