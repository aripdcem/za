package com.za.games.ui.turetme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.turetme.TuretmeState
import com.za.games.turetme.TuretmeWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlin.random.Random

enum class TuretmeMode { DAILY, FREE }

class TuretmeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TuretmeStore(application)

    private val _mode = MutableStateFlow(TuretmeMode.DAILY)
    val mode: StateFlow<TuretmeMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow(restoredDaily())
    val state: StateFlow<TuretmeState> = _state.asStateFlow()

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    /** Günün turu; aynı gün içinde bulunmuş kelimeler (ve pes) geri oynatılır. */
    private fun restoredDaily(): TuretmeState {
        val day = todayEpoch()
        var state = TuretmeState.daily(TuretmeWords.bases, TuretmeWords.valid, day)
        if (store.dailyDay == day) {
            for (word in store.dailyFound) {
                state = state.restoreFound(word)
            }
            if (store.dailyGivenUp) {
                state = state.giveUp()
            }
        }
        return state
    }

    /** Gün değiştiyse günlük turu tazeler (ekran öne gelişinde çağrılır). */
    fun refreshDaily() {
        if (_mode.value == TuretmeMode.DAILY && _state.value.dailyDay != todayEpoch()) {
            _state.value = restoredDaily()
        }
    }

    fun setMode(mode: TuretmeMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        _state.value = when (mode) {
            TuretmeMode.DAILY -> restoredDaily()
            TuretmeMode.FREE -> TuretmeState.free(TuretmeWords.bases, TuretmeWords.valid)
        }
    }

    fun pick(index: Int) = _state.update { it.pick(index) }

    fun erase() = _state.update { it.erase() }

    fun clearCurrent() = _state.update { it.clearCurrent() }

    fun shuffle() = _state.update { it.shuffle(Random.nextLong()) }

    fun submit() {
        val before = _state.value
        val after = before.submit()
        _state.value = after
        if (after.found.size == before.found.size) return // geçersiz gönderim

        val day = after.dailyDay
        if (_mode.value == TuretmeMode.DAILY && day != null && day == todayEpoch()) {
            store.saveDaily(day, after.found)
        }
    }

    /** Pes: tur biter, bulunamayan kelimeler açıklanır. Günlükte kalıcıdır. */
    fun giveUp() {
        val before = _state.value
        val after = before.giveUp()
        if (after == before) return
        _state.value = after

        val day = after.dailyDay
        if (_mode.value == TuretmeMode.DAILY && day != null && day == todayEpoch()) {
            store.saveDaily(day, after.found, givenUp = true)
        }
    }

    /** Serbest modda yeni taban kelime. */
    fun newFreeGame() {
        if (_mode.value == TuretmeMode.FREE) {
            _state.value = TuretmeState.free(TuretmeWords.bases, TuretmeWords.valid)
        }
    }
}
