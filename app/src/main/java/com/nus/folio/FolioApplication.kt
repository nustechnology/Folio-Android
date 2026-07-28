package com.nus.folio

import android.app.Application
import com.nus.folio.di.AppContainer

class FolioApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer()
    }
}
