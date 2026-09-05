package com.za.games.platform

import androidx.annotation.StringRes
import com.za.games.R

/** Ana menüdeki oyun grupları; süzgeç çipleri bu sırayla gösterilir. */
enum class GameCategory(@StringRes val labelRes: Int) {
    WORD(R.string.hub_cat_word),
    PUZZLE(R.string.hub_cat_puzzle),
    ARCADE(R.string.hub_cat_arcade),
    BOARD(R.string.hub_cat_board),
}
