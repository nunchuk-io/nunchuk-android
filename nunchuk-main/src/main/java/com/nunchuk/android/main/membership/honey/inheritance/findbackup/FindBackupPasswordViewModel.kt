package com.nunchuk.android.main.membership.honey.inheritance.findbackup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class FindBackupPasswordViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<FindBackupPasswordEvent>()
    val event = _event.asSharedFlow()

}

sealed class FindBackupPasswordEvent