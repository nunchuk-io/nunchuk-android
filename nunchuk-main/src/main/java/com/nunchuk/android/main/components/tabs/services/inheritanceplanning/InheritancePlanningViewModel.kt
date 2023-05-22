package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InheritancePlanningViewModel @Inject constructor() : ViewModel() {
    lateinit var setupOrReviewParam: InheritancePlanningParam.SetupOrReview
        private set

    fun setOrUpdate(param: InheritancePlanningParam) {
        if (param is InheritancePlanningParam.SetupOrReview) {
            setupOrReviewParam = param
        }
    }
}

sealed class InheritancePlanningParam {
    data class SetupOrReview(
        val activationDate: Long = 0L,
        val walletId: String,
        val emails: List<String> = emptyList(),
        val isNotify: Boolean = false,
        val magicalPhrase: String = "",
        val bufferPeriod: Period? = null,
        val note: String = "",
        val verifyToken: String = "",
        val planFlow: Int = 0,
        val isOpenFromWizard: Boolean = false,
        val groupId: String = ""
    ) : InheritancePlanningParam()
}

