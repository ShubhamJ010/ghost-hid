package com.sj010.ghid.data

import android.content.Context
import com.sj010.ghid.GhostHidApp
import com.sj010.ghid.domain.model.QuickSendEntry
import com.sj010.ghid.domain.model.QuickSendHistoryPolicy
import com.sj010.ghid.domain.model.QuickSendTime
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Persists dispatched quick-send schemes so the composer can offer rerun and
 * "save as combo" without asking the user to retype anything.
 */
class QuickSendHistoryRepository(private val context: Context) {

    private val store: JsonListStore<QuickSendEntry>
        get() = JsonListStore(
            File(context.getExternalFilesDir(null), FILE_NAME),
            QuickSendEntry.serializer()
        )

    private val _entries = MutableStateFlow<List<QuickSendEntry>>(emptyList())
    val entries: StateFlow<List<QuickSendEntry>> = _entries.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        _entries.value = store.read()
    }

    fun record(scheme: String) {
        val limit = try {
            GhostHidApp.instance.settingsRepository.settings.value.quickSendHistoryLimit
        } catch (_: Exception) { QuickSendHistoryPolicy.MAX_ENTRIES }

        val updated = QuickSendHistoryPolicy.record(
            current = _entries.value,
            scheme = scheme,
            createdAt = QuickSendTime.isoNow(),
            maxEntries = limit
        )
        _entries.value = updated
        persist(updated)
    }

    fun clear() {
        _entries.value = emptyList()
        persist(emptyList())
    }

    private fun persist(items: List<QuickSendEntry>) {
        scope.launch { store.write(items) }
    }

    private companion object {
        const val FILE_NAME = "quick_send_history.json"
    }
}
