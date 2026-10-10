package com.apolloauto.player.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.companion.CompanionVoice

class UsbDetachReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_EJECT ||
            intent?.action == "android.hardware.usb.action.USB_DEVICE_DETACHED"
        ) {
            val line = CompanionVoice.usbOutLine()
            ApolloApp.instance.themeStore.speak(line)
            ApolloApp.instance.libraryStore.setStatus(line)
        }
    }
}
