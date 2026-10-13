package com.yourapp.whatsappclone

import android.app.Application
import android.content.Context
import com.yourapp.whatsappclone.data.JaWaManager
import com.yourapp.whatsappclone.data.SessionStore
import java.io.File

/**
 * Application entry point. JaWa stores its session files under the path we give
 * it via FileAuthStore, so we keep sessions under filesDir/jiwa-session/.
 */
class WhatsAppApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        JaWaManager.init(this)
    }

    companion object {
        private lateinit var instance: WhatsAppApp

        fun from(context: Context): WhatsAppApp =
            context.applicationContext as WhatsAppApp
    }
}
