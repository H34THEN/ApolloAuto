package com.apolloauto.player.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apolloauto.player.ApolloApp

class UsbDetachReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_EJECT ||
            intent?.action == "android.hardware.usb.action.USB_DEVICE_DETACHED"
        ) {
            ApolloApp.instance.libraryStore.setStatus("USB detached — reconnect SSD")
        }
    }
}
