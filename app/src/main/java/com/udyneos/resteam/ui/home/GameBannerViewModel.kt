package com.udyneos.resteam.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.data.GameBannerStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Terpisah dari HomeViewModel agar logika Home yang sudah ada tidak berubah. */
@HiltViewModel
class GameBannerViewModel @Inject constructor(
    private val store: GameBannerStore,
) : ViewModel() {
    val banners: StateFlow<Map<String, File>> = store.banners

    fun setBanner(packageName: String, uri: android.net.Uri, onError: (String) -> Unit) {
        viewModelScope.launch {
            store.save(packageName, uri).onFailure { onError(it.message ?: "Gagal mengatur banner") }
        }
    }

    fun clearBanner(packageName: String) {
        viewModelScope.launch { store.clear(packageName) }
    }
}
