package com.hermes.downloader

import android.app.Application
import com.hermes.downloader.data.ServiceLocator
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YTDowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
