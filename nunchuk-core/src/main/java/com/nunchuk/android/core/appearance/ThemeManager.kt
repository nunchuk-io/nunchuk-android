package com.nunchuk.android.core.appearance

import com.nunchuk.android.usecase.darkmode.GetDarkModeUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    getDarkModeUseCase: GetDarkModeUseCase,
    applicationScope: CoroutineScope,
) {
    val mode = getDarkModeUseCase(Unit)
        .map { it.getOrThrow() }
        .stateIn(applicationScope, SharingStarted.Lazily, null)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemeEntrypoint {
    fun provideThemeManager(): ThemeManager
}