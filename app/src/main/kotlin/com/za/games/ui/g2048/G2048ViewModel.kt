package com.za.games.ui.g2048

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.g2048.G2048State
import com.za.games.g2048.G2048Status
import com.za.games.g2048.MoveDir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 2048 sıra tabanlıdır; yerçekimi döngüsü yoktur. Tahta cihazda saklanır. */
class G2048ViewModel(application: Application) : AndroidViewModel(application) {

    private val store = G2048Store(application)

    private val _state = MutableStateFlow(store.restore() ?: G2048State.newGame())
    val state: StateFlow<G2048State> = _state.asStateFlow()

    fun move(dir: MoveDir) {
        _state.update { it.move(dir) }
        persist()
    }

    fun newGame() {
        _state.value = G2048State.newGame()
        persist()
    }

    private fun persist() {
        val current = _state.value
        if (current.status == G2048Status.OVER) store.clear() else store.save(current)
    }
}
