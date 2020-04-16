package org.covidwatch.android.ble

import android.app.*
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import org.covidwatch.android.R
import org.covidwatch.android.ui.MainActivity
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothServiceCallback
import org.tcncoalition.tcnclient.bluetooth.TcnLifecycleService
import org.tcncoalition.tcnclient.bluetooth.TcnLifecycleService.LocalBinder

abstract class BluetoothManager {
    open fun startService() {}
    open fun stopService() {}
}

class BluetoothManagerImpl(
    private val app: Application,
    val tcnBluetoothServiceCallback: TcnBluetoothServiceCallback
) : BluetoothManager() {

    companion object {
        private const val CHANNEL_ID = "COVIDWatchContactTracingNotificationChannel"
    }

    private val intent get() = Intent(app, TcnLifecycleService::class.java)

    private var service: TcnLifecycleService? = null
    private var binded = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@BluetoothManagerImpl.service = (service as LocalBinder).service.apply {
                setTcnCallback(tcnBluetoothServiceCallback)
                setForegroundNotification(foregroundNotification())
                startTcnBluetoothService()
            }
            binded = true
        }

        override fun onServiceDisconnected(name: ComponentName?)  {
            binded = false
        }
    }

    private fun foregroundNotification(): Notification {
        createNotificationChannelIfNeeded()

        val notificationIntent = Intent(app, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            app, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(app, CHANNEL_ID)
            .setContentTitle(app.getString(R.string.foreground_notification_title))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun startService() {
        app.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun stopService() {
        if (binded) {
            service?.stopTcnBluetoothService()
            app.unbindService(serviceConnection)
            binded = false
        }
    }

    /**
     * This notification channel is only required for android versions above
     * android O. This creates the necessary notification channel for the foregroundService
     * to function.
     */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                app, NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
