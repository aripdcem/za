package com.aripd.zagames.ui.kiskac

import com.aripd.zagames.sozluk.WordLang
import com.aripd.zagames.platform.WordLangs
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aripd.zagames.besharf.BesHarfWords
import com.aripd.zagames.kiskac.KiskacState
import com.aripd.zagames.kiskac.KiskacStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class KiskacMode { DAILY, FREE }

/** Kıskaç, Beş Harf'in kelime listelerini paylaşır. */
class KiskacViewModel(application: Application) : AndroidViewModel(application) {

    private val store = KiskacStore(application)

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
        sortWords()
    }

    private val _mode = MutableStateFlow(KiskacMode.DAILY)
    val mode: StateFlow<KiskacMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow(restoredDaily())
    val state: StateFlow<KiskacState> = _state.asStateFlow()

    private val _streak = MutableStateFlow(store.streak)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _easyMode = MutableStateFlow(store.easyMode)
    val easyMode: StateFlow<Boolean> = _easyMode.asStateFlow()

    fun setEasyMode(value: Boolean) {
        store.easyMode = value
        _easyMode.value = value
    }

    /** Uzaklık ipucu için tahmin edilebilir tüm kelimeler Türkçe sırayla; arka planda dizilir. */
    private val _sortedWords = MutableStateFlow<List<String>>(emptyList())
    val sortedWords: StateFlow<List<String>> = _sortedWords.asStateFlow()

    init {
        sortWords()
    }

    /**
     * Uzaklık ipucunun ikili araması için liste dilin sözlük sırasında olmalı.
     * Dosya zaten o sırada yazılıyor, yine de burada doğrulanıp kullanılır:
     * okuma ve ilk dizim arka planda, ana iş parçacığı tıkanmasın diye.
     */
    private fun sortWords() {
        val lang = _wordLang.value
        val source = words
        _sortedWords.value = emptyList()
        viewModelScope.launch(Dispatchers.Default) {
            val sorted = source.allowed
            if (_wordLang.value == lang) _sortedWords.value = sorted
        }
    }

    private fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    /** Günün bulmacası; aynı gün içinde kaydedilmiş tahminler geri oynatılır. */
    private fun restoredDaily(): KiskacState {
        val day = todayEpoch()
        var state = KiskacState.daily(_wordLang.value, words.answers, day)
        if (store.dailyDay(_wordLang.value.tag) == day) {
            for (guess in store.dailyGuesses(_wordLang.value.tag)) {
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
            KiskacMode.FREE -> KiskacState.free(_wordLang.value, words.answers)
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
        if (_mode.value == KiskacMode.DAILY && day != null && day == todayEpoch()) {
            store.saveDaily(_wordLang.value.tag, day, after.guesses.map { it.word })
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

    /** Dil değişince açık tahta o dilde yeniden kurulur. */
    private fun restart() {
        _state.value = when (_mode.value) {
            KiskacMode.DAILY -> restoredDaily()
            KiskacMode.FREE -> KiskacState.free(_wordLang.value, words.answers)
        }
    }

    /** Serbest modda yeni kelime. */
    fun newFreeGame() {
        if (_mode.value == KiskacMode.FREE) {
            _state.value = KiskacState.free(_wordLang.value, words.answers)
        }
    }
}
