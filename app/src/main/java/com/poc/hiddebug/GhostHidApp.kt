package com.poc.hiddebug

import android.app.Application
import com.poc.hiddebug.data.AndroidKeystoreVault
import com.poc.hiddebug.data.ComboRepository
import com.poc.hiddebug.data.QuickSendHistoryRepository
import com.poc.hiddebug.data.SnippetRepository

class GhostHidApp : Application() {

    lateinit var vault: AndroidKeystoreVault
        private set
    lateinit var comboRepository: ComboRepository
        private set
    lateinit var snippetRepository: SnippetRepository
        private set
    lateinit var quickSendHistoryRepository: QuickSendHistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        vault = AndroidKeystoreVault(this)
        comboRepository = ComboRepository(this)
        snippetRepository = SnippetRepository(this, vault)
        quickSendHistoryRepository = QuickSendHistoryRepository(this)

        // Seed default combos and snippets asynchronously
        comboRepository.ensureDefaults()
        snippetRepository.ensureDefaults()
    }

    companion object {
        lateinit var instance: GhostHidApp
            private set
    }
}
