package ru.fbear.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.android.synthetic.main.activity_main.*

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        (context as MainActivity).uri.text = intent.getStringExtra("ru.fbear.lab7.broadcast.Message")
    }
}