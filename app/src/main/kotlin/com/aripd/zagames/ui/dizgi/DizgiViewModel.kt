package com.aripd.zagames.ui.dizgi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aripd.zagames.dizgi.DizgiState
import com.aripd.zagames.dizgi.DizgiStatus
import com.aripd.zagames.dizgi.DizgiWords
import com.aripd.zagames.platform.WordLangs
import com.aripd.zagames.sozluk.WordLang
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Ekran evresi: kurulum → (el değişimi ⇄ oyun) → bitiş oyun içinde gösterilir. */
enum class DizgiPhase { SETUP, HANDOVER, PLAY }

class DizgiViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Oyunun kelime dili. Varsayılan arayüzün dilidir; oyuncular kurulum
     * kartından başka bir dil seçebilir ve seçim kalıcıdır (bkz. [WordLangs]).
     * Dil hem sözlüğü hem torbadaki harf dağılımını ve puanları değiştirir.
     */
    private val _wordLang = MutableStateFlow(WordLangs.current(application))
    val wordLang: StateFlow<WordLang> = _wordLang.asStateFlow()

    private var words = DizgiWords.of(_wordLang.value)

    /** Seçim: null = arayüzün dilini izle. Kurulum ekranında değiştirilir. */
    fun setWordLang(lang: WordLang?) {
        WordLangs.choose(getApplication(), lang)
        val next = WordLangs.current(getApplication())
        if (next == _wordLang.value) return
        _wordLang.value = next
        words = DizgiWords.of(next)
        warmUp()
    }

    private val _phase = MutableStateFlow(DizgiPhase.SETUP)
    val phase: StateFlow<DizgiPhase> = _phase.asStateFlow()

    /** Yeni maç sayacı: ekran efekt korumaları bunu anahtar olarak kullanır. */
    private val _matchId = MutableStateFlow(0)
    val matchId: StateFlow<Int> = _matchId.asStateFlow()

    private val _state = MutableStateFlow(DizgiState.new(_wordLang.value, 2, Random.nextLong()))
    val state: StateFlow<DizgiState> = _state.asStateFlow()

    init {
        warmUp()
    }

    /**
     * Sözlük ilk "Onayla"da ana iş parçacığında yüklenmesin; oyuncu daha
     * kurulum ekranındayken arka planda okunur. Dil değişince yinelenir.
     */
    private fun warmUp() {
        val source = words
        viewModelScope.launch(Dispatchers.Default) { source.valid.size }
    }

    fun start(playerCount: Int) {
        _state.value = DizgiState.new(_wordLang.value, playerCount, Random.nextLong())
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

    fun submit() = commit { it.submit(words.valid::contains) }

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
