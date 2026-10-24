package com.sj010.ghid

import android.app.Application
import com.sj010.ghid.data.AndroidKeystoreVault
import com.sj010.ghid.data.ComboRepository
import com.sj010.ghid.data.ComboUsageRepository
import com.sj010.ghid.data.DeviceOsCache
import com.sj010.ghid.data.PinnedFavoritesRepository
import com.sj010.ghid.data.QuickSendHistoryRepository
import com.sj010.ghid.data.SnippetRepository

class GhostHidApp : Application() {

    lateinit var vault: AndroidKeystoreVault
        private set
    lateinit var comboRepository: ComboRepository
        private set
    lateinit var comboUsageRepository: ComboUsageRepository
        private set
    lateinit var pinnedFavoritesRepository: PinnedFavoritesRepository
        private set
    lateinit var snippetRepository: SnippetRepository
        private set
    lateinit var quickSendHistoryRepository: QuickSendHistoryRepository
        private set
    lateinit var deviceOsCache: DeviceOsCache
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        vault = AndroidKeystoreVault(this)
        comboRepository = ComboRepository(this)
        comboUsageRepository = ComboUsageRepository(this)
        pinnedFavoritesRepository = PinnedFavoritesRepository(this)
        snippetRepository = SnippetRepository(this, vault)
        quickSendHistoryRepository = QuickSendHistoryRepository(this)
        deviceOsCache = DeviceOsCache(this)

        // Seed default combos and snippets asynchronously
        comboRepository.ensureDefaults()
        snippetRepository.ensureDefaults()
    }

    companion object {
        lateinit var instance: GhostHidApp
            private set
    }
}
