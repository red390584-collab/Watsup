package com.yourapp.whatsappclone

import android.app.Application
import android.content.Context

/**
 * Application entry point. The WhatsApp session is owned by the WebView
 * (cookies + DOM storage), so there is no native bridge to initialize here.
 */
class WhatsAppApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private lateinit var instance: WhatsAppApp

        fun from(context: Context): WhatsAppApp =
            context.applicationContext as WhatsAppApp
    }
}
