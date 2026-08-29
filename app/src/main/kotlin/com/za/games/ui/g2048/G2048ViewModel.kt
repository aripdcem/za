package com.za.games.ui.g2048

import androidx.lifecycle.ViewModel
import com.za.games.g2048.G2048State
import com.za.games.g2048.MoveDir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 2048 sıra tabanlıdır; yerçekimi döngüsü yoktur. */
class G2048ViewModel : ViewModel() {

    private val _state = MutableStateFlow(G2048State.newGame())
    val state: StateFlow<G2048State> = _state.asStateFlow()

    fun move(dir: MoveDir) = _state.update { it.move(dir) }

    fun newGame() = _state.update { G2048State.newGame() }
}
