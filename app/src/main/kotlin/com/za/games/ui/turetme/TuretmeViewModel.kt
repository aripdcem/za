package com.za.games.ui.turetme

import com.za.games.sozluk.WordLang
import com.za.games.platform.WordLangs
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.za.games.turetme.TuretmeState
import com.za.games.turetme.TuretmeWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

enum class TuretmeMode { DAILY, FREE }

class TuretmeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TuretmeStore(application)

    /**
     * Oyunun kelime dili. Varsayılan arayüzün dilidir; oyuncu kurulum kartından
     * başka bir dil seçebilir ve seçim kalıcıdır (bkz. [WordLangs]).
     */
    private val _wordLang = MutableStateFlow(WordLangs.current(application))
    val wordLang: StateFlow<WordLang> = _wordLang.asStateFlow()

    private var words = TuretmeWords.of(_wordLang.value)

    /** Seçim: null = arayüzün dilini izle. Dil değişince tur baştan kurulur. */
    fun setWordLang(lang: WordLang?) {
        WordLangs.choose(getApplication(), lang)
        val next = WordLangs.current(getApplication())
        if (next == _wordLang.value) return
        _wordLang.value = next
        words = TuretmeWords.of(next)
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.Default) {
                if (_mode.value == TuretmeMode.DAILY) restoredDaily()
                else TuretmeState.free(words.bases, words.valid, Random.nextLong())
            }
            _state.value = fresh
        }
    }

    private val _mode = MutableStateFlow(TuretmeMode.DAILY)
    val mode: StateFlow<TuretmeMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow(restoredDaily())
    val state: StateFlow<TuretmeState> = _state.asStateFlow()

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    /** Günün turu; aynı gün içinde bulunmuş kelimeler (ve pes) geri oynatılır. */
    private fun restoredDaily(): TuretmeState {
        val day = todayEpoch()
        var state = TuretmeState.daily(words.bases, words.valid, day)
        if (store.dailyDay(_wordLang.value.tag) == day) {
            for (word in store.dailyFound(_wordLang.value.tag)) {
                state = state.restoreFound(word)
            }
            if (store.dailyGivenUp(_wordLang.value.tag)) {
                state = state.giveUp()
            }
        }
        return state
    }

    /** Gün değiştiyse günlük turu tazeler (ekran öne gelişinde çağrılır). */
    fun refreshDaily() {
        if (_mode.value != TuretmeMode.DAILY || _state.value.dailyDay == todayEpoch()) return
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.Default) { restoredDaily() }
            if (_mode.value == TuretmeMode.DAILY) _state.value = fresh
        }
    }

    fun setMode(mode: TuretmeMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        // Hedef kelime taraması (15k+ kelime) ana iş parçacığını tıkamasın.
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.Default) {
                when (mode) {
                    TuretmeMode.DAILY -> restoredDaily()
                    TuretmeMode.FREE -> TuretmeState.free(words.bases, words.valid)
                }
            }
            // Hesap sürerken mod yeniden değiştiyse bu sonuç bayattır.
            if (_mode.value == mode) _state.value = fresh
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
            store.saveDaily(_wordLang.value.tag, day, after.found)
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
            store.saveDaily(_wordLang.value.tag, day, after.found, givenUp = true)
        }
    }

    /** Serbest modda yeni taban kelime. */
    fun newFreeGame() {
        if (_mode.value != TuretmeMode.FREE) return
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                TuretmeState.free(words.bases, words.valid)
            }
        }
    }
}
