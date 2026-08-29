package com.za.games.ui.besharf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.besharf.BesHarfState
import com.za.games.besharf.BesHarfStatus
import com.za.games.besharf.BesHarfWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class BesHarfMode { DAILY, FREE }

class BesHarfViewModel(application: Application) : AndroidViewModel(application) {

    private val store = BesHarfStore(application)

    private val _mode = MutableStateFlow(BesHarfMode.DAILY)
    val mode: StateFlow<BesHarfMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow(restoredDaily())
    val state: StateFlow<BesHarfState> = _state.asStateFlow()

    private val _streak = MutableStateFlow(store.streak)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    /** Günün bulmacası; aynı gün içinde kaydedilmiş tahminler geri oynatılır. */
    private fun restoredDaily(): BesHarfState {
        val day = todayEpoch()
        var state = BesHarfState.daily(BesHarfWords.answers, day)
        if (store.dailyDay == day) {
            for (guess in store.dailyGuesses) {
                state = state.copy(current = guess).submit { true }
            }
        }
        return state
    }

    fun setMode(mode: BesHarfMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        _state.value = when (mode) {
            BesHarfMode.DAILY -> restoredDaily()
            BesHarfMode.FREE -> BesHarfState.free(BesHarfWords.answers)
        }
    }

    fun type(letter: Char) = _state.update { it.type(letter) }

    fun erase() = _state.update { it.erase() }

    fun submit() {
        val before = _state.value
        val after = before.submit(BesHarfWords::isAllowed)
        _state.value = after
        if (after.guesses.size == before.guesses.size) return // geçersiz gönderim

        val day = after.dailyDay
        if (_mode.value == BesHarfMode.DAILY && day != null && day == todayEpoch()) {
            store.saveDaily(day, after.guesses)
        }
        if (before.status == BesHarfStatus.RUNNING) {
            when (after.status) {
                BesHarfStatus.WON -> {
                    store.streak += 1
                    _streak.value = store.streak
                }
                BesHarfStatus.LOST -> {
                    store.streak = 0
                    _streak.value = 0
                }
                BesHarfStatus.RUNNING -> Unit
            }
        }
    }

    /** Serbest modda yeni kelime. */
    fun newFreeGame() {
        if (_mode.value == BesHarfMode.FREE) {
            _state.value = BesHarfState.free(BesHarfWords.answers)
        }
    }
}
