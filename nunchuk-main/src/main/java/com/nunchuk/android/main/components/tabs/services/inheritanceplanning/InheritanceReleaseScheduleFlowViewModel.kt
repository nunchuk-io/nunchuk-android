package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.lifecycle.ViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.model.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class InheritanceReleaseScheduleDraft(
    val releaseScheduleUiState: ReleaseScheduleUiState = ReleaseScheduleUiState(),
    val pendingNewStage: ReleaseScheduleStage? = null,
    val bufferPeriod: Period? = null,
    val bufferPeriodApplyType: InheritanceBufferPeriodApplyType? = null,
    val hasBufferPeriodSelection: Boolean = false,
)

data class InheritanceReleaseScheduleFlowState(
    val drafts: Map<String, InheritanceReleaseScheduleDraft> = mapOf(
        InheritanceReleaseScheduleFlowViewModel.DEFAULT_DRAFT_ID to InheritanceReleaseScheduleDraft()
    ),
    val activeDraftId: String = InheritanceReleaseScheduleFlowViewModel.DEFAULT_DRAFT_ID,
)

@HiltViewModel
class InheritanceReleaseScheduleFlowViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(InheritanceReleaseScheduleFlowState())
    val state = _state.asStateFlow()

    private var nextDraftId = 0L

    val releaseScheduleUiState: ReleaseScheduleUiState
        get() = getDraft().releaseScheduleUiState

    val pendingNewStage: ReleaseScheduleStage?
        get() = getDraft().pendingNewStage

    fun getDraft(draftId: String? = null): InheritanceReleaseScheduleDraft {
        val resolvedId = resolveDraftId(draftId)
        return _state.value.drafts[resolvedId] ?: InheritanceReleaseScheduleDraft()
    }

    fun setActiveDraftId(draftId: String) {
        ensureDraft(draftId)
        _state.update { it.copy(activeDraftId = draftId) }
    }

    fun createDraft(
        releaseScheduleUiState: ReleaseScheduleUiState = ReleaseScheduleUiState(),
        bufferPeriod: Period? = null,
        bufferPeriodApplyType: InheritanceBufferPeriodApplyType? = null,
        hasBufferPeriodSelection: Boolean = false,
    ): String {
        val draftId = generateDraftId()
        _state.update {
            it.copy(
                activeDraftId = draftId,
                drafts = it.drafts + mapOf(
                    draftId to InheritanceReleaseScheduleDraft(
                        releaseScheduleUiState = releaseScheduleUiState,
                        pendingNewStage = null,
                        bufferPeriod = bufferPeriod,
                        bufferPeriodApplyType = bufferPeriodApplyType,
                        hasBufferPeriodSelection = hasBufferPeriodSelection,
                    )
                )
            )
        }
        return draftId
    }

    fun cloneDraft(sourceDraftId: String): String {
        val source = getDraft(sourceDraftId)
        val draftId = generateDraftId()
        _state.update {
            it.copy(
                activeDraftId = draftId,
                drafts = it.drafts + mapOf(
                    draftId to source.copy(pendingNewStage = null)
                )
            )
        }
        return draftId
    }

    fun setReleaseScheduleUiState(value: ReleaseScheduleUiState) {
        setReleaseScheduleUiState(resolveDraftId(null), value)
    }

    fun setReleaseScheduleUiState(draftId: String, value: ReleaseScheduleUiState) {
        updateDraft(draftId) { it.copy(releaseScheduleUiState = value) }
    }

    fun setPendingNewStage(value: ReleaseScheduleStage?) {
        setPendingNewStage(resolveDraftId(null), value)
    }

    fun setPendingNewStage(draftId: String, value: ReleaseScheduleStage?) {
        updateDraft(draftId) { it.copy(pendingNewStage = value) }
    }

    fun setBufferPeriodSelection(
        draftId: String,
        period: Period?,
    ) {
        updateDraft(draftId) {
            it.copy(
                bufferPeriod = period,
                bufferPeriodApplyType = if (period == null) null else it.bufferPeriodApplyType,
                hasBufferPeriodSelection = true,
            )
        }
    }

    fun setBufferPeriodApplyType(
        draftId: String,
        applyType: InheritanceBufferPeriodApplyType?,
    ) {
        updateDraft(draftId) {
            it.copy(
                bufferPeriodApplyType = applyType,
                hasBufferPeriodSelection = true,
            )
        }
    }

    fun discardDraft(draftId: String) {
        if (draftId == DEFAULT_DRAFT_ID) return
        _state.update {
            val updatedDrafts = it.drafts - draftId
            val fallbackActive = if (it.activeDraftId == draftId) {
                updatedDrafts.keys.firstOrNull() ?: DEFAULT_DRAFT_ID
            } else {
                it.activeDraftId
            }
            it.copy(
                drafts = if (updatedDrafts.isEmpty()) {
                    mapOf(DEFAULT_DRAFT_ID to InheritanceReleaseScheduleDraft())
                } else {
                    updatedDrafts
                },
                activeDraftId = fallbackActive,
            )
        }
    }

    fun clearAllTransientDrafts() {
        _state.update {
            it.copy(
                drafts = mapOf(DEFAULT_DRAFT_ID to InheritanceReleaseScheduleDraft()),
                activeDraftId = DEFAULT_DRAFT_ID,
            )
        }
    }

    private fun updateDraft(
        draftId: String,
        transform: (InheritanceReleaseScheduleDraft) -> InheritanceReleaseScheduleDraft,
    ) {
        val resolvedId = resolveDraftId(draftId)
        ensureDraft(resolvedId)
        _state.update { flowState ->
            val currentDraft = flowState.drafts[resolvedId] ?: InheritanceReleaseScheduleDraft()
            flowState.copy(
                drafts = flowState.drafts + mapOf(resolvedId to transform(currentDraft))
            )
        }
    }

    private fun ensureDraft(draftId: String) {
        _state.update { state ->
            if (state.drafts.containsKey(draftId)) {
                state
            } else {
                state.copy(
                    drafts = state.drafts + mapOf(draftId to InheritanceReleaseScheduleDraft())
                )
            }
        }
    }

    private fun resolveDraftId(draftId: String?): String {
        return draftId?.takeIf { it.isNotBlank() } ?: _state.value.activeDraftId
    }

    private fun generateDraftId(): String {
        nextDraftId += 1L
        return "draft_$nextDraftId"
    }

    companion object {
        const val DEFAULT_DRAFT_ID = "default"
    }
}
