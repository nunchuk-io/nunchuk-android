/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.domain.di

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.CreateShareFileUseCaseImpl
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetChainTipUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NativeCommonModule {

    @Binds
    fun bindGetChainTipUseCase(useCase: GetChainTipUseCaseImpl): GetChainTipUseCase

    @Binds
    fun bindCreateShareFileUseCase(useCase: CreateShareFileUseCaseImpl): CreateShareFileUseCase

    companion object {

        @Singleton
        @Provides
        fun provideNativeSdk() = NativeSdkProvider.instance.nativeSdk

    }
}

class NativeSdkProvider {
    val nativeSdk = NunchukNativeSdk()

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = NativeSdkProvider()
    }
}


