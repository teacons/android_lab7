package ru.fbear.lab7

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        download_btn.setOnClickListener {
            val urlText =
                if (url.text.isNotEmpty()) {
                    url.text.toString()
                } else {
                    "https://old.fbear.ru/image/android_cat.webp"
                }
            val intent = Intent(this, MyService::class.java)
            intent.putExtra("url", urlText)
            startService(intent)
        }
    }
}

