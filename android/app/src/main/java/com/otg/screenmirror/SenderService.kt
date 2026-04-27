package com.otg.screenmirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log

class SenderService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaCodec: MediaCodec? = null

    private var usbManager: UsbManager? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var outputStream: java.io.FileOutputStream? = null

    private var isStreaming = false
    private var thread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, "OTG_SENDER_CHANNEL")
            .setContentTitle("Screen Mirroring")
            .setContentText("Sharing screen via USB...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }

        val resultCode = intent?.getIntExtra("code", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, data)
            
            setupUsbAccessory()
            startScreenCapture()
        }

        return START_NOT_STICKY
    }

    private fun setupUsbAccessory() {
        val accessories = usbManager?.accessoryList
        if (!accessories.isNullOrEmpty()) {
            val accessory = accessories[0]
            if (usbManager?.hasPermission(accessory) == true) {
                fileDescriptor = usbManager?.openAccessory(accessory)
                if (fileDescriptor != null) {
                    val fd = fileDescriptor!!.fileDescriptor
                    outputStream = java.io.FileOutputStream(fd)
                }
            }
        }
    }

    private fun startScreenCapture() {
        try {
            val width = 1280
            val height = 720
            val dpi = 320

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "OTGMirror", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null
            )

            isStreaming = true
            thread = Thread {
                val bufferInfo = MediaCodec.BufferInfo()
                while (isStreaming) {
                    val outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                    if (outIndex >= 0) {
                        val encodedData = mediaCodec?.getOutputBuffer(outIndex)
                        if (encodedData != null && bufferInfo.size != 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            
                            val bytes = ByteArray(bufferInfo.size)
                            encodedData.get(bytes)
                            
                            try {
                                // Write directly to USB Accessory stream!
                                outputStream?.write(bytes)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // If write fails, connection might be lost
                                break
                            }
                        }
                        mediaCodec?.releaseOutputBuffer(outIndex, false)
                    }
                }
            }
            thread?.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "OTG_SENDER_CHANNEL",
            "Screen Mirroring Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        isStreaming = false
        try {
            thread?.join(1000)
        } catch (e: Exception) {}
        
        virtualDisplay?.release()
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaProjection?.stop()
        
        outputStream?.close()
        fileDescriptor?.close()
    }
}
