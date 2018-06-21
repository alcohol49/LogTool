package com.aoitek.logtool

import android.os.Handler
import android.os.Message
import java.lang.ref.WeakReference

class MessageHandler(activity: MainActivity) : Handler() {

    private val mainActivity: WeakReference<MainActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message) {
        val mainActivity = mainActivity.get() ?: return
        when (msg.what) {
            MSG_REFRESH_LIST -> {
                mainActivity.refreshList()
            }
        }
    }
}