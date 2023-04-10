package com.nunchuk.android.wallet.components.coin.tag

object CoinTagColorUtil {

    private val coinTagColors = arrayListOf<CoinTagColor>()
    val hexColors = arrayListOf<String>()

    init {
        hexColors.apply {
            add("#9EC063")
            add("#2F466C")
            add("#FAA077")
            add("#1C652D")
            add("#B4DCFF")
            add("#7E519B")
            add("#FDD95C")
            add("#595959")
            add("#D38FFF")
            add("#CF4018")
            add("#FFFFFF")
            add("#A66800")
        }
        getCoinTagColors()
    }

    private fun getCoinTagColors() {
        hexColors.forEachIndexed { index, color ->
            coinTagColors.add(CoinTagColor(index, color))
        }
    }
}

data class CoinTagColor(val index: Int, val value: String)
