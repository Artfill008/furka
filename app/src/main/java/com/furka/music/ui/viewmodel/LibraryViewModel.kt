package com.furka.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.furka.music.data.model.AudioTrack
import com.furka.music.data.repository.AudioRepository
import com.furka.music.util.AlphanumericComparator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * LIBRARY VIEW MODEL
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Manages library state with:
 * - MediaStore audio track loading
 * - Search/filter functionality (by title or artist)
 * - Shuffle support (returns random track)
 */
sealed interface LibraryUiState {
    object Loading : LibraryUiState
    object PermissionDenied : LibraryUiState
    data class Success(
        val tracks: List<AudioTrack>,
        val allTracks: List<AudioTrack>, // Unfiltered for shuffle
        val searchQuery: String = "",
        val fastScrollIndex: Map<Char, Int> = emptyMap(),
        val sections: List<Char> = emptyList()
    ) : LibraryUiState
    object Empty : LibraryUiState
}

@OptIn(FlowPreview::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AudioRepository(application)
    
    // All tracks (unfiltered)
    private val _allTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    
    // Search query with debounce for performance
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Base UI state
    private val _baseState = MutableStateFlow<BaseState>(BaseState.Loading)
    
    // Combined UI state with search filtering
    val uiState: StateFlow<LibraryUiState> = combine(
        _baseState,
        _allTracks,
        _searchQuery.debounce(300) // Debounce search for performance
    ) { baseState, allTracks, query ->
        when (baseState) {
            is BaseState.Loading -> LibraryUiState.Loading
            is BaseState.PermissionDenied -> LibraryUiState.PermissionDenied
            is BaseState.Loaded -> {
                val filtered = if (query.isBlank()) {
                    allTracks
                } else {
                    allTracks.filter { track ->
                        track.title.contains(query, ignoreCase = true) ||
                        track.artist.contains(query, ignoreCase = true)
                    }
                }
                
                if (allTracks.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    // Generate Fast Scroll Index for the filtered list
                    val indexMap = mutableMapOf<Char, Int>()
                    val sectionList = mutableListOf<Char>()
                    
                    filtered.forEachIndexed { index, track ->
                        val firstChar = track.title.firstOrNull()?.uppercaseChar() ?: '#'
                        val sectionChar = if (firstChar.isLetter()) firstChar else '#'
                        
                        if (!indexMap.containsKey(sectionChar)) {
                            indexMap[sectionChar] = index
                            sectionList.add(sectionChar)
                        }
                    }

                    LibraryUiState.Success(
                        tracks = filtered,
                        allTracks = allTracks,
                        searchQuery = query,
                        fastScrollIndex = indexMap,
                        sections = sectionList
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState.Loading
    )

    fun loadLibrary() {
        viewModelScope.launch {
            _baseState.value = BaseState.Loading
            try {
                val rawTracks = repository.getAudioTracks()
                val comparator = AlphanumericComparator()
                val sortedTracks = rawTracks.sortedWith { t1, t2 -> 
                    comparator.compare(t1.title, t2.title)
                }
                
                _allTracks.value = sortedTracks
                _baseState.value = BaseState.Loaded
            } catch (e: SecurityException) {
                _baseState.value = BaseState.PermissionDenied
            } catch (e: Exception) {
                _allTracks.value = emptyList()
                _baseState.value = BaseState.Loaded
            }
        }
    }
    
    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            loadLibrary()
        } else {
            _baseState.value = BaseState.PermissionDenied
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHUFFLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun getRandomTrack(): AudioTrack? {
        return _allTracks.value.randomOrNull()
    }

    fun getAllTracks(): List<AudioTrack> {
        return _allTracks.value
    }
}

// Internal base state (before search filtering)
private sealed interface BaseState {
    object Loading : BaseState
    object PermissionDenied : BaseState
    object Loaded : BaseState
}
