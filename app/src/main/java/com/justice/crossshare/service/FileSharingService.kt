package com.justice.crossshare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.justice.crossshare.MainActivity
import com.justice.crossshare.R
import com.justice.crossshare.network.CrossShareServer
import com.justice.crossshare.network.NetworkUtils
import com.justice.crossshare.utils.QrCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service that manages the lifecycle of CrossShare Ktor HTTP Server,
 * holds Wi-Fi lock, maintains notification, and exposes server state / QR code.
 */
class FileSharingService : Service() {

    companion object {
        const val CHANNEL_ID = "file_sharing_channel"
        const val CHANNEL_NAME = "CrossShare Server"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVER = "com.justice.crossshare.action.START_SERVER"
        const val ACTION_STOP_SERVER = "com.justice.crossshare.action.STOP_SERVER"
        const val EXTRA_PORT = "com.justice.crossshare.extra.PORT"

        private val _isServerRunning = MutableStateFlow(false)
        val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

        private val _serverUrl = MutableStateFlow<String?>(null)
        val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

        private val _serverPort = MutableStateFlow(8080)
        val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

        private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
        val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()

        private val _qrCodeImageBitmap = MutableStateFlow<ImageBitmap?>(null)
        val qrCodeImageBitmap: StateFlow<ImageBitmap?> = _qrCodeImageBitmap.asStateFlow()

        fun startService(context: Context, port: Int = 8080) {
            val intent = Intent(context, FileSharingService::class.java).apply {
                action = ACTION_START_SERVER
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FileSharingService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): FileSharingService = this@FileSharingService
        val isServerRunningState: StateFlow<Boolean> get() = isServerRunning
        val serverUrlState: StateFlow<String?> get() = serverUrl
        val serverPortState: StateFlow<Int> get() = serverPort
        val qrCodeBitmapState: StateFlow<Bitmap?> get() = qrCodeBitmap
        val qrCodeImageBitmapState: StateFlow<ImageBitmap?> get() = qrCodeImageBitmap
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: CrossShareServer? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080

        when (action) {
            ACTION_START_SERVER -> {
                startServer(port)
            }
            ACTION_STOP_SERVER -> {
                stopServer()
            }
            else -> {
                if (!_isServerRunning.value) {
                    startServer(port)
                }
            }
        }

        return START_STICKY
    }

    private fun startServer(port: Int) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                if (server == null) {
                    server = CrossShareServer(applicationContext)
                }

                server?.start(port)
                val actualPort = server?.serverPort?.value ?: port
                val url = NetworkUtils.getFormattedServerUrl(actualPort)
                val qrBitmap = url?.let { QrCodeGenerator.generateQrCode(it) }
                val qrImageBitmap = url?.let { QrCodeGenerator.generateQrCodeImageBitmap(it) }

                _isServerRunning.value = true
                _serverUrl.value = url
                _serverPort.value = actualPort
                _qrCodeBitmap.value = qrBitmap
                _qrCodeImageBitmap.value = qrImageBitmap

                acquireWifiLock()

                val notification = buildNotification(url)
                startForegroundWithServiceType(notification)
                println("FileSharingService: Server started successfully at $url")
            } catch (e: Exception) {
                e.printStackTrace()
                println("FileSharingService: Failed to start server: ${e.message}")
                _isServerRunning.value = false
                _serverUrl.value = null
                _serverPort.value = port
                _qrCodeBitmap.value = null
                _qrCodeImageBitmap.value = null
                try {
                    server?.stop()
                } catch (ignored: Exception) {}
                server = null
                releaseWifiLock()
                try {
                    ServiceCompat.stopForeground(this@FileSharingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun stopServer() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                server?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                server = null
                releaseWifiLock()

                _isServerRunning.value = false
                _serverUrl.value = null
                _serverPort.value = 8080
                _qrCodeBitmap.value = null
                _qrCodeImageBitmap.value = null

                try {
                    ServiceCompat.stopForeground(this@FileSharingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                println("FileSharingService: Server stopped successfully")
            }
        }
    }

    private fun acquireWifiLock() {
        try {
            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
                wifiLock = wifiManager?.createWifiLock(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                    } else {
                        @Suppress("DEPRECATION")
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF
                    },
                    "CrossShare:ServerWifiLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            wifiLock?.acquire()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wifiLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "CrossShare Server active status"
                setShowBadge(false)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(url: String?): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FileSharingService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (url != null) {
            "Server active at $url"
        } else {
            "Server starting..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CrossShare Server")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop Server",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundWithServiceType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                serviceType
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            server?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        releaseWifiLock()
        _isServerRunning.value = false
        _serverUrl.value = null
        _serverPort.value = 8080
        _qrCodeBitmap.value = null
        _qrCodeImageBitmap.value = null
        serviceScope.cancel()
    }
}
