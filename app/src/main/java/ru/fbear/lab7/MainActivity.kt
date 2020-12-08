package ru.fbear.lab7

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWNLOAD_URL -> {
                    Glide.with(this@MainActivity).load(msg.data.getString(URL)).into(result)
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
        mMessenger = Messenger(IncomingHandler())
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
            sendBounded(prepURL())
        }
        download_started.setOnClickListener {
            sendStarted(prepURL())
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

    private fun prepURL() =
        if (url.text.isNotEmpty()) {
            url.text.toString()
        } else {
            "https://old.fbear.ru/image/android_cat.webp"
        }


}

