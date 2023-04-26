package com.nunchuk.android.wallet.components.coin.filter

import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.android.parcel.Parcelize
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CoinFilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinFilterFragmentArgs =
        CoinFilterFragmentArgs.fromSavedStateHandle(savedStateHandle)
    val selectTags = mutableStateOf(args.filter.selectTags)
    val selectCollections = mutableStateOf(args.filter.selectCollections)
    val startTime = mutableStateOf(args.filter.startTime)
    val endTime = mutableStateOf(args.filter.endTime)

    fun setSelectedTags(tagIds: IntArray) {
        selectTags.value = tagIds.toSet()
    }

    fun setSelectedCollection(collectionIds: IntArray) {
        selectCollections.value = collectionIds.toSet()
    }

    fun setDate(isStart: Boolean, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        if (isStart) {
            cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            startTime.value = cal.timeInMillis
        } else {
            cal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            endTime.value = cal.timeInMillis
        }
    }
}

@Parcelize
data class CoinFilterUiState(
    val selectTags: Set<Int> = emptySet(),
    val selectCollections: Set<Int> = emptySet(),
    val startTime: Long = -1,
    val endTime: Long = -1,
    val min: String = "",
    val isMinBtc: Boolean = false,
    val max: String = "",
    val isMaxBtc: Boolean = false,
    val showLockedCoin: Boolean = true,
    val showUnlockedCoin: Boolean = true,
    val isDescending: Boolean = true,
) : Parcelable