package com.guddiz.app

import android.app.Application
import com.guddiz.app.di.AppContainer

class GuddizApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
