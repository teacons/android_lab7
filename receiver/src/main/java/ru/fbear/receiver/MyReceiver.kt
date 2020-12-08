package ru.fbear.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.getStringExtra("ru.fbear.lab7.broadcast.Message")?.let {
            (context as MainActivity).replaceImageView(it)
        }
    }
}