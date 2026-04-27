package com.otg.screenmirror

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val screenCaptureIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, SenderService::class.java).apply {
                putExtra("code", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(serviceIntent)
            Toast.makeText(this, "Screen mirroring started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Screen cast permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.btnStartSender).setOnClickListener {
            startSender()
        }

        findViewById<Button>(R.id.btnStartReceiver).setOnClickListener {
            startReceiver()
        }
    }

    private fun startSender() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureIntentLauncher.launch(captureIntent)
    }

    private fun startReceiver() {
        val intent = Intent(this, ReceiverActivity::class.java)
        startActivity(intent)
    }
}
