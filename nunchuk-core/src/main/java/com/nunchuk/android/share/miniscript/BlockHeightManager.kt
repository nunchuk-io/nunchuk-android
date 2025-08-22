package com.nunchuk.android.share.miniscript

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

interface BlockHeightManager {
    val state: StateFlow<Int>
    val currentBlock: Int
        get() = state.value
}

@Singleton
internal class BlockHeightManagerImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    coroutineScope: CoroutineScope
) : BlockHeightManager {
    private val _state = MutableStateFlow(0)
    override val state = _state.asStateFlow()

    init {
        coroutineScope.launch {
            while (true) {
                val blockHeight = nativeSdk.getChainTip()
                Timber.d("Current block height: $blockHeight")
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

val Context.currentBlock: Int
    get() = MegaNavigatorProvider.get(this).currentBlock

@Composable
fun rememberBlockHeightManager(): BlockHeightManager {
    val context = LocalContext.current
    val isInEditMode = LocalView.current.isInEditMode
    return if (isInEditMode) {
        remember {
            object : BlockHeightManager {
                override val state: StateFlow<Int> = MutableStateFlow(0).asStateFlow()
            }
        }
    } else {
        remember { MegaNavigatorProvider.get(context) }
    }
}
