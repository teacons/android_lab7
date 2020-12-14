package ru.fbear.lab7

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    class IncomingHandler(linkToActivity: WeakReference<MainActivity>) : Handler(Looper.getMainLooper()) {
        val activity = linkToActivity.get()
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWNLOAD_URL -> {
                    if (activity != null) {
                        activity.result.text = msg.data.getString(URL)
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    companion object {
        private const val DOWNLOAD_URL = 1
        private const val URL = "url"
    }

    private lateinit var mMessenger: Messenger

    private var mService: Messenger? = null
    private var bound: Boolean = false

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = Messenger(service)
            bound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            bound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MyService::class.java).also { intent ->
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
        mMessenger = Messenger(IncomingHandler(WeakReference(this)))
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(mConnection)
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        download_bounded.setOnClickListener {
            val urlText =
                if (url.text.isNotEmpty()) {
                    url.text.toString()
                } else {
                    "https://old.fbear.ru/image/android_cat.webp"
                }
            sendBounded(urlText)
        }
        download_started.setOnClickListener {
            val urlText =
                if (url.text.isNotEmpty()) {
                    url.text.toString()
                } else {
                    "https://old.fbear.ru/image/android_cat.webp"
                }
            sendStarted(urlText)
        }
    }

    private fun sendBounded(url: String) {
        if (!bound) return
        val bundle = Bundle().apply {
            putString(URL, url)
        }
        val msg: Message = Message.obtain(null, DOWNLOAD_URL, 0, 0).apply {
            data = bundle
            replyTo = mMessenger
        }
        try {
            mService?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendStarted(url: String) {
        val intent = Intent(this, MyService::class.java)
        intent.putExtra("url", url)
        startService(intent)

    }

}

