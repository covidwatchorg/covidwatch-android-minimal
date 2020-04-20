package org.covidwatch.android.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import org.covidwatch.android.R
import org.covidwatch.android.ui.MainActivity
import org.tcncoalition.tcnclient.bluetooth.BluetoothStateListener
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothService
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothService.LocalBinder
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothServiceCallback

abstract class BluetoothManager {
    open fun startService() {}
    open fun stopService() {}
}

// https://robertohuertas.com/2019/06/29/android_foreground_services/
class BluetoothManagerImpl(
    private val context: Context,
    val tcnBluetoothServiceCallback: TcnBluetoothServiceCallback
) : BluetoothManager(), BluetoothStateListener {

    private var service: TcnBluetoothService? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isBound = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@BluetoothManagerImpl.service = (service as LocalBinder).service.apply {
                val notification = foregroundNotification(
                    context.getString(R.string.foreground_notification_title)
                )
                startForegroundNotificationIfNeeded(NOTIFICATION_ID, notification)
                setBluetoothStateListener(this@BluetoothManagerImpl)
                startTcnExchange(tcnBluetoothServiceCallback)
                // We need this lock so our service gets not affected by Doze Mode
                wakeLock =
                    (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TcnBluetoothService::lock").apply {
                            acquire()
                        }
                    }
            }
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private fun foregroundNotification(title: String): Notification {
        createNotificationChannelIfNeeded()

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun startService() {
        context.bindService(
            Intent(context, TcnBluetoothService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun stopService() {
        if (isBound) {
            service?.stopTcnExchange()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                service?.stopForeground(true)
            }
            context.unbindService(serviceConnection)
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            isBound = false
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
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(
                context, NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "CovidWatchContactTracingNotificationChannel"
        const val NOTIFICATION_ID = 1 // Don't use 0
    }

    override fun bluetoothStateChanged(bluetoothOn: Boolean) {
        val title = if (bluetoothOn) {
            R.string.foreground_notification_title
        } else {
            R.string.foreground_notification_ble_off
        }

        getSystemService(
            context,
            NotificationManager::class.java
        )?.notify(
            NOTIFICATION_ID,
            foregroundNotification(context.getString(title))
        )
    }
}
