package com.za.games.ui.kiskac

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.za.games.besharf.BesHarfWords
import com.za.games.kiskac.KiskacState
import com.za.games.kiskac.KiskacStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class KiskacMode { DAILY, FREE }

/** Kıskaç, Beş Harf'in kelime listelerini paylaşır. */
class KiskacViewModel(application: Application) : AndroidViewModel(application) {

    private val store = KiskacStore(application)

    private val _mode = MutableStateFlow(KiskacMode.DAILY)
    val mode: StateFlow<KiskacMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow(restoredDaily())
    val state: StateFlow<KiskacState> = _state.asStateFlow()

    private val _streak = MutableStateFlow(store.streak)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    /** Günün bulmacası; aynı gün içinde kaydedilmiş tahminler geri oynatılır. */
    private fun restoredDaily(): KiskacState {
        val day = todayEpoch()
        var state = KiskacState.daily(BesHarfWords.answers, day)
        if (store.dailyDay == day) {
            for (guess in store.dailyGuesses) {
                state = state.copy(current = guess).submit { true }
            }
        }
        return state
    }

    /** Gün değiştiyse günlük tahtayı tazeler (ekran öne gelişinde çağrılır). */
    fun refreshDaily() {
        if (_mode.value == KiskacMode.DAILY && _state.value.dailyDay != todayEpoch()) {
            _state.value = restoredDaily()
        }
    }

    fun setMode(mode: KiskacMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        _state.value = when (mode) {
            KiskacMode.DAILY -> restoredDaily()
            KiskacMode.FREE -> KiskacState.free(BesHarfWords.answers)
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
        if (_mode.value == KiskacMode.DAILY && day != null && day == todayEpoch()) {
            store.saveDaily(day, after.guesses.map { it.word })
        }
        if (before.status == KiskacStatus.RUNNING) {
            when (after.status) {
                KiskacStatus.WON -> {
                    store.streak += 1
                    _streak.value = store.streak
                }
                KiskacStatus.LOST -> {
                    store.streak = 0
                    _streak.value = 0
                }
                KiskacStatus.RUNNING -> Unit
            }
        }
    }

    /** Serbest modda yeni kelime. */
    fun newFreeGame() {
        if (_mode.value == KiskacMode.FREE) {
            _state.value = KiskacState.free(BesHarfWords.answers)
        }
    }
}
