package com.nunchuk.android.share.miniscript

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class BlockHeightManager @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    coroutineScope: CoroutineScope
) {
    private val _state = MutableStateFlow(0)
    val state = _state.asStateFlow()

    init {
        coroutineScope.launch {
            while (true) {
                val blockHeight = nativeSdk.getChainTip()
                _state.update { blockHeight }
                delay(1.minutes)
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface BlockHeightManagerEntryPoint {
    val blockHeightManager: BlockHeightManager
}

internal object MegaNavigatorProvider {
    private val blockHeightManagerRef = AtomicReference<BlockHeightManager?>(null)

    fun get(context: Context): BlockHeightManager {
        return blockHeightManagerRef.get() ?: run {
            val newNavigator = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BlockHeightManagerEntryPoint::class.java
            ).blockHeightManager

            blockHeightManagerRef.set(newNavigator)
            newNavigator
        }
    }
}

@Composable
fun rememberBlockHeightManager(): BlockHeightManager {
    val context = LocalContext.current
    return remember { MegaNavigatorProvider.get(context) }
}
