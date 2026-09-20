package com.za.games.ui.besharf

import com.za.games.sozluk.WordLang
import com.za.games.platform.WordLangs
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

    /**
     * Oyunun kelime dili. Varsayılan arayüzün dilidir; oyuncu kurulum kartından
     * başka bir dil seçebilir ve seçim kalıcıdır (bkz. [WordLangs]).
     */
    private val _wordLang = MutableStateFlow(WordLangs.current(application))
    val wordLang: StateFlow<WordLang> = _wordLang.asStateFlow()

    /** Seçili dilin listeleri; dil değişince yeniden okunur (önbellekli). */
    private var words = BesHarfWords.of(_wordLang.value)

    /** Seçim: null = arayüzün dilini izle. Dil değişince oyun baştan kurulur. */
    fun setWordLang(lang: WordLang?) {
        WordLangs.choose(getApplication(), lang)
        val next = WordLangs.current(getApplication())
        if (next == _wordLang.value) return
        _wordLang.value = next
        words = BesHarfWords.of(next)
        restart()
    }

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
        var state = BesHarfState.daily(_wordLang.value, words.answers, day)
        if (store.dailyDay(_wordLang.value.tag) == day) {
            for (guess in store.dailyGuesses(_wordLang.value.tag)) {
                state = state.copy(current = guess).submit { true }
            }
        }
        return state
    }

    /**
     * Gün değiştiyse günlük tahtayı tazeler. ViewModel etkinlik ömrünce
     * yaşadığından ekrana yeniden girişte/öne gelişte çağrılır; yoksa gece
     * yarısından sonra bir önceki günün bulmacası görünmeye devam eder.
     */
    fun refreshDaily() {
        if (_mode.value == BesHarfMode.DAILY && _state.value.dailyDay != todayEpoch()) {
            _state.value = restoredDaily()
        }
    }

    fun setMode(mode: BesHarfMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        _state.value = when (mode) {
            BesHarfMode.DAILY -> restoredDaily()
            BesHarfMode.FREE -> BesHarfState.free(_wordLang.value, words.answers)
        }
    }

    fun type(letter: Char) = _state.update { it.type(letter) }

    fun erase() = _state.update { it.erase() }

    fun submit() {
        val before = _state.value
        val after = before.submit(words::isAllowed)
        _state.value = after
        if (after.guesses.size == before.guesses.size) return // geçersiz gönderim

        val day = after.dailyDay
        if (_mode.value == BesHarfMode.DAILY && day != null && day == todayEpoch()) {
            store.saveDaily(_wordLang.value.tag, day, after.guesses)
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

    /** Dil değişince açık tahta o dilde yeniden kurulur. */
    private fun restart() {
        _state.value = when (_mode.value) {
            BesHarfMode.DAILY -> restoredDaily()
            BesHarfMode.FREE -> BesHarfState.free(_wordLang.value, words.answers)
        }
    }

    /** Serbest modda yeni kelime. */
    fun newFreeGame() {
        if (_mode.value == BesHarfMode.FREE) {
            _state.value = BesHarfState.free(_wordLang.value, words.answers)
        }
    }
}
