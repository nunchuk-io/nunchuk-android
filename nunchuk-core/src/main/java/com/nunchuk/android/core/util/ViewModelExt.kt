package com.nunchuk.android.core.util

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import kotlinx.coroutines.launch

fun <S, E, T> NunchukViewModel<S, E>.process(
    func: suspend () -> Result<T>,
    doOnSuccess: (T) -> Unit = {},
    doOnError: (Exception) -> Unit = {}
) {
    viewModelScope.launch {
        when (val result = func()) {
            is Success<T> -> doOnSuccess(result.data)
            is Error -> doOnError(result.exception)
        }
    }
}
