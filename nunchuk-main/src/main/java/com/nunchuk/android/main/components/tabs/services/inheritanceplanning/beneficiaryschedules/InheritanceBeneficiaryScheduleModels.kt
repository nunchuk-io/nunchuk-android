package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBufferPeriodApplyType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.model.Period

data class InheritanceBeneficiaryScheduleConfig(
    val releaseScheduleUiState: ReleaseScheduleUiState,
    val bufferPeriod: Period?,
    val bufferPeriodApplyType: InheritanceBufferPeriodApplyType?,
)

data class InheritanceBeneficiaryScheduleCardData(
    val releaseScheduleUiState: ReleaseScheduleUiState,
    val bufferPeriodSummaryText: String?,
)
