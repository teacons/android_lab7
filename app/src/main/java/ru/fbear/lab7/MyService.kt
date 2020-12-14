package ru.fbear.lab7

import android.app.Service
import android.content.Intent
import android.os.*
import android.webkit.URLUtil
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.URL


class MyService : Service() {

    private lateinit var mMessenger: Messenger

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        runBlocking {
            val path = downloadImage(intent.getStringExtra(URL))
            path?.let { broadcast(it) }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler(WeakReference(this)))
        return mMessenger.binder
    }

    companion object {
        private const val DOWNLOAD_URL = 1
        private const val URL = "url"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadImage(urlString: String?): String? {
        return withContext(Dispatchers.IO) {
            var outputStream: FileOutputStream? = null
            try {
                val url = URL(urlString)
                val connect = url.openConnection()
                val mimeType = connect.contentType
                val inputStream = connect.getInputStream()
                val filename = URLUtil.guessFileName(connect.url.toString(), null, mimeType)
                outputStream = openFileOutput(filename, MODE_PRIVATE)
                outputStream.write(inputStream.readBytes())
                "$filesDir/$filename"
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                outputStream?.close()
            }
        }
    }

    private fun broadcast(path: String) {
        val i = Intent().apply {
            action = "ru.fbear.lab7.DOWNLOAD_COMPLETE"
            putExtra("ru.fbear.lab7.broadcast.Message", path)
        }
        sendBroadcast(i)
    }

    class IncomingHandler(linkToActivity: WeakReference<MyService>) : Handler(Looper.getMainLooper()) {
        private val service = linkToActivity.get()
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWNLOAD_URL -> {
                    runBlocking {
                        val path = service?.downloadImage(msg.data.getString(URL))
                        path.let {
                            val bundle = Bundle().apply {
                                putString(URL, it)
                            }
                            val mes: Message = Message.obtain(null, DOWNLOAD_URL).apply {
                                data = bundle
                            }
                            try {
                                val repMessenger = msg.replyTo
                                repMessenger.send(mes)
                            } catch (e: RemoteException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }
}