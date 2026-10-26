package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.domain.model.PinnedFavorite
import com.sj010.ghid.domain.model.PinnedFavoritesPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Persists which favorite combo sits on which dashboard shape.
 * Starts empty so the collage always begins as a set of add slots.
 */
class PinnedFavoritesRepository(private val context: Context) {

    private val store: JsonListStore<PinnedFavorite>
        get() = JsonListStore(
            File(context.getExternalFilesDir(null), FILE_NAME),
            PinnedFavorite.serializer()
        )

    private val _pins = MutableStateFlow<Map<Int, String>>(emptyMap())
    val pins: StateFlow<Map<Int, String>> = _pins.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        _pins.value = store.read().associate { it.slotId to it.comboName }
    }

    fun pin(slotId: Int, comboName: String) {
        val updated = PinnedFavoritesPolicy.pin(_pins.value, slotId, comboName)
        if (updated == _pins.value) return
        _pins.value = updated
        persist(updated)
    }

    fun unpin(slotId: Int) {
        val updated = PinnedFavoritesPolicy.unpin(_pins.value, slotId)
        if (updated == _pins.value) return
        _pins.value = updated
        persist(updated)
    }

    private fun persist(pins: Map<Int, String>) {
        val entries = pins
            .map { (slotId, comboName) -> PinnedFavorite(slotId = slotId, comboName = comboName) }
            .sortedBy { it.slotId }
        scope.launch { store.write(entries) }
    }

    private companion object {
        const val FILE_NAME = "pinned_favorites.json"
    }
}
