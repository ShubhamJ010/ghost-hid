package com.sj010.ghid

import android.app.Application
import com.sj010.ghid.data.AndroidKeystoreVault
import com.sj010.ghid.data.ComboRepository
import com.sj010.ghid.data.ComboUsageRepository
import com.sj010.ghid.data.DeviceOsCache
import com.sj010.ghid.data.PayloadRepository
import com.sj010.ghid.data.PinnedFavoritesRepository
import com.sj010.ghid.data.QuickSendHistoryRepository

class GhostHidApp : Application() {

    lateinit var vault: AndroidKeystoreVault
        private set
    lateinit var payloadRepository: PayloadRepository
        private set
    lateinit var comboRepository: ComboRepository
        private set
    lateinit var comboUsageRepository: ComboUsageRepository
        private set
    lateinit var pinnedFavoritesRepository: PinnedFavoritesRepository
        private set
    lateinit var quickSendHistoryRepository: QuickSendHistoryRepository
        private set
    lateinit var deviceOsCache: DeviceOsCache
        private set

    /** Backward compatibility alias */
    val snippetRepository: PayloadRepository
        get() = payloadRepository

    override fun onCreate() {
        super.onCreate()
        instance = this

        vault = AndroidKeystoreVault(this)
        payloadRepository = PayloadRepository(this, vault)
        comboRepository = ComboRepository(this, payloadRepository)
        comboUsageRepository = ComboUsageRepository(this)
        pinnedFavoritesRepository = PinnedFavoritesRepository(this)
        quickSendHistoryRepository = QuickSendHistoryRepository(this)
        deviceOsCache = DeviceOsCache(this)

        // Seed default payloads and combos asynchronously
        payloadRepository.ensureDefaults()
        comboRepository.ensureDefaults()
    }

    companion object {
        lateinit var instance: GhostHidApp
            private set
    }
}
