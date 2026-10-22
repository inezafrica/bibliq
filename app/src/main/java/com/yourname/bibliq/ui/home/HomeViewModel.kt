package com.yourname.bibliq.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.bibliq.data.repository.ContentRepository
import com.yourname.bibliq.data.repository.ContentStatus
import com.yourname.bibliq.data.repository.GameHomeRepository
import com.yourname.bibliq.data.repository.HomeUiState
import com.yourname.bibliq.data.repository.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val content = ContentRepository.get(application)
    private val repository = GameHomeRepository(application, content, ProgressRepository.get(application))

    val uiState: StateFlow<HomeUiState?> = repository.observeHome()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val contentStatus: StateFlow<ContentStatus> = content.status

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { content.refresh() }
    }

    fun onSettingsClick() {
        Log.d(TAG, "Settings clicked")
    }

    fun onNavClick(destination: String) {
        Log.d(TAG, "Nav clicked: $destination")
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
