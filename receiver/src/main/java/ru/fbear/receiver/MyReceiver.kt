package ru.fbear.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import kotlinx.android.synthetic.main.activity_main.*

class MyReceiver : BroadcastReceiver() {

    val uri = MutableLiveData<String>()

    override fun onReceive(context: Context, intent: Intent) {
        uri.value = intent.getStringExtra("ru.fbear.lab7.broadcast.Message")
    }
}