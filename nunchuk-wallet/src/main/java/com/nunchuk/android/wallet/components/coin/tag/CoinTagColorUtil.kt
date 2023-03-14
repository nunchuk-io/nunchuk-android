package com.nunchuk.android.wallet.components.coin.tag

object CoinTagColorUtil {

    val coinTagColors = arrayListOf<CoinTagColor>()
    val hexColors = arrayListOf<String>()

    init {
        hexColors.apply {
            add("#CF4018")
            add("#FAA077")
            add("#A66800")
            add("#FDD95C")
            add("#1C652D")
            add("#9EC063")
            add("#B4DCFF")
            add("#2F466C")
            add("#7E519B")
            add("#D38FFF")
            add("#FFFFFF")
            add("#595959")
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
