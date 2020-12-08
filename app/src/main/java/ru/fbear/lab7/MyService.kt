package ru.fbear.lab7

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.net.URL


class MyService : Service() {

    private lateinit var mMessenger: Messenger

    private var count = 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        runBlocking {
            val path = downloadImage(intent.getStringExtra(URL), "$count.png")
            count++
            path?.let { broadcast(it) }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler())
        return mMessenger.binder
    }

    companion object {
        private const val DOWNLOAD_URL = 1
        private const val URL = "url"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadImage(urlString: String?, filename: String): String? {
        return withContext(Dispatchers.IO) {
            var outputStream: FileOutputStream? = null
            try {
                val url = URL(urlString)
                val inputStream = url.openStream()
                val img = BitmapFactory.decodeStream(inputStream)
                outputStream = openFileOutput(filename, MODE_PRIVATE)
                img.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
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
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        sendBroadcast(i)
    }

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWNLOAD_URL -> {
                    runBlocking {
                        val path = downloadImage(msg.data.getString(URL), "$count.png")
                        count++
                        path?.let {
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