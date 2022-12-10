package com.nunchuk.android.transaction.components.schedule.timezone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SelectTimeZoneViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<SelectTimeZoneEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(SelectTimeZoneState())
    val state = _state.asStateFlow()

    private val timezones = mutableListOf<TimeZoneDetail>()
    private val search = MutableSharedFlow<String>()

    init {
        val timezones = TimeZone.getAvailableIDs().mapNotNull { id ->
            id.toTimeZoneDetail()
        }.sortedBy { it.offset }
        this.timezones.apply {
            clear()
            addAll(timezones)
        }
        _state.update {
            it.copy(timezones = timezones)
        }
        viewModelScope.launch {
            search.debounce(300L).collect { value ->
                if (value.isEmpty()) {
                    _state.update {
                        it.copy(timezones = timezones)
                    }
                } else {
                    _state.update {
                        it.copy(timezones = timezones.filter { timeZone ->
                            timeZone.city.contains(value)
                                    || timeZone.country.contains(value)
                                    || timeZone.offset.contains(value)
                        })
                    }
                }
            }
        }
    }

    fun onSearch(value: String) {
        viewModelScope.launch {
            search.emit(value)
        }
    }

    fun onTimeZoneClicked(zone: TimeZoneDetail) {
        viewModelScope.launch {
            _event.emit(SelectTimeZoneEvent.OnSelectTimeZone(zone))
        }
    }
}

sealed class SelectTimeZoneEvent {
    data class OnSelectTimeZone(val zone: TimeZoneDetail,) : SelectTimeZoneEvent()
}