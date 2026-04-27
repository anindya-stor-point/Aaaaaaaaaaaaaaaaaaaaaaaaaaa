package com.otg.screenmirror

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReceiverActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var usbManager: UsbManager
    private var usbConnection: UsbDeviceConnection? = null
    private var usbEndpoint: UsbEndpoint? = null

    private lateinit var surfaceView: SurfaceView
    private lateinit var tvStatus: TextView
    private var mediaCodec: MediaCodec? = null

    private var isPlaying = false
    private var thread: Thread? = null

    companion object {
        private const val ACTION_USB_PERMISSION = "com.otg.screenmirror.USB_PERMISSION"
        private const val TAG = "ReceiverActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)

        surfaceView = findViewById(R.id.surfaceView)
        tvStatus = findViewById(R.id.tvStatus)
        surfaceView.holder.addCallback(this)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initDecoder(holder)
        checkUsbDevices()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopPlayback()
    }

    private fun checkUsbDevices() {
        val deviceList = usbManager.deviceList
        if (deviceList.isNotEmpty()) {
            val device = deviceList.values.first()
            if (usbManager.hasPermission(device)) {
                connectToDevice(device)
            } else {
                val permissionIntent = PendingIntent.getBroadcast(
                    this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(device, permissionIntent)
            }
        } else {
            tvStatus.text = "No USB devices found. Connect via OTG."
        }
    }

    private fun connectToDevice(device: UsbDevice) {
        val intrf = device.getInterface(0)
        usbConnection = usbManager.openDevice(device)
        usbConnection?.claimInterface(intrf, true)
        
        // Find bulk IN endpoint
        for (i in 0 until intrf.endpointCount) {
            val ep = intrf.getEndpoint(i)
            // IN endpoint because we are receiving
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && 
                ep.direction == UsbConstants.USB_DIR_IN) {
                usbEndpoint = ep
                break
            }
        }

        if (usbEndpoint != null) {
            tvStatus.visibility = View.GONE
            startPlaybackThread()
        } else {
            tvStatus.text = "Compatible USB endpoint not found."
        }
    }

    private fun initDecoder(holder: SurfaceHolder) {
        try {
            // Setup MediaCodec for H264
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720)
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, holder.surface, null, 0)
            mediaCodec?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlaybackThread() {
        if (isPlaying) return
        isPlaying = true
        thread = Thread {
            val buffer = ByteArray(16384)
            val info = MediaCodec.BufferInfo()
            
            while (isPlaying && usbConnection != null && usbEndpoint != null) {
                // Read from USB
                val bytesRead = usbConnection!!.bulkTransfer(usbEndpoint, buffer, buffer.size, 1000)
                if (bytesRead > 0) {
                    try {
                        val inIndex = mediaCodec!!.dequeueInputBuffer(10000)
                        if (inIndex >= 0) {
                            val byteBuffer = mediaCodec!!.getInputBuffer(inIndex)
                            byteBuffer?.clear()
                            byteBuffer?.put(buffer, 0, bytesRead)
                            mediaCodec!!.queueInputBuffer(inIndex, 0, bytesRead, System.nanoTime() / 1000, 0)
                        }

                        var outIndex = mediaCodec!!.dequeueOutputBuffer(info, 10000)
                        while (outIndex >= 0) {
                            mediaCodec!!.releaseOutputBuffer(outIndex, true)
                            outIndex = mediaCodec!!.dequeueOutputBuffer(info, 0)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        thread?.start()
    }

    private fun stopPlayback() {
        isPlaying = false
        try {
            thread?.join(1000)
        } catch (e: Exception) {}
        
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaCodec = null

        usbConnection?.close()
        usbConnection = null
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { connectToDevice(it) }
                    } else {
                        tvStatus.text = "USB permission denied"
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                stopPlayback()
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = "Device disconnected"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        stopPlayback()
    }
}
