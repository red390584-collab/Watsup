package com.yourapp.whatsappclone

import android.app.Application
import android.content.Context
import com.yourapp.whatsappclone.data.CobaltManager
import com.yourapp.whatsappclone.data.SessionStore
import java.io.File

/**
 * Application entry point. Cobalt is a desktop-first JVM library that derives its
 * session storage from `user.home`; we repoint that to the app's private files
 * directory before any Cobalt class is loaded so sessions persist on Android.
 */
class WhatsAppApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        System.setProperty("user.home", filesDir.absolutePath)
        File(filesDir, "cobalt").mkdirs()
        CobaltManager.init(this)
    }

    companion object {
        private lateinit var instance: WhatsAppApp

        fun from(context: Context): WhatsAppApp =
            context.applicationContext as WhatsAppApp
    }
}
