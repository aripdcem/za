package com.za.games.ui.dizgi

import androidx.lifecycle.ViewModel
import com.za.games.dizgi.DizgiState
import com.za.games.dizgi.DizgiStatus
import com.za.games.dizgi.DizgiWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** Ekran evresi: kurulum → (el değişimi ⇄ oyun) → bitiş oyun içinde gösterilir. */
enum class DizgiPhase { SETUP, HANDOVER, PLAY }

class DizgiViewModel : ViewModel() {

    private val _phase = MutableStateFlow(DizgiPhase.SETUP)
    val phase: StateFlow<DizgiPhase> = _phase.asStateFlow()

    /** Yeni maç sayacı: ekran efekt korumaları bunu anahtar olarak kullanır. */
    private val _matchId = MutableStateFlow(0)
    val matchId: StateFlow<Int> = _matchId.asStateFlow()

    private val _state = MutableStateFlow(DizgiState.new(2, Random.nextLong()))
    val state: StateFlow<DizgiState> = _state.asStateFlow()

    fun start(playerCount: Int) {
        _state.value = DizgiState.new(playerCount, Random.nextLong())
        _matchId.value += 1
        _phase.value = DizgiPhase.HANDOVER
    }

    fun beginTurn() {
        if (_phase.value == DizgiPhase.HANDOVER) _phase.value = DizgiPhase.PLAY
    }

    fun toSetup() {
        _phase.value = DizgiPhase.SETUP
    }

    fun place(cell: Int, rackIndex: Int, jokerAs: Char? = null) {
        _state.value = _state.value.place(cell, rackIndex, jokerAs)
    }

    fun recall(cell: Int) {
        _state.value = _state.value.recall(cell)
    }

    fun recallAll() {
        _state.value = _state.value.recallAll()
    }

    fun submit() = commit { it.submit(DizgiWords.valid::contains) }

    fun pass() = commit { it.pass() }

    fun exchange(rackIndices: List<Int>) = commit { it.exchange(rackIndices) }

    /** Hamleyi uygular; sıra geçtiyse el değişim perdesine döner. */
    private fun commit(move: (DizgiState) -> DizgiState) {
        val before = _state.value
        val after = move(before)
        _state.value = after
        if (after.moveCount > before.moveCount && after.status == DizgiStatus.RUNNING) {
            _phase.value = DizgiPhase.HANDOVER
        }
    }
}
