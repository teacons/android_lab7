package ru.fbear.receiver

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val broadcastReceiver = MyReceiver()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val filter = IntentFilter("ru.fbear.lab7.DOWNLOAD_COMPLETE")
        registerReceiver(broadcastReceiver, filter)
        broadcastReceiver.uri.observe(this) {
            replaceImageView(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun replaceImageView(uri: String?) {
        if (haveStoragePermission()) {
            Glide.with(this)
                .load(uri)
                .error(R.drawable.ic_broken_image)
                .into(image)
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions,
                READ_EXTERNAL_STORAGE_REQUEST
            )
        }
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val READ_EXTERNAL_STORAGE_REQUEST = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> replaceImageView(broadcastReceiver.uri.value)
        }
    }
}