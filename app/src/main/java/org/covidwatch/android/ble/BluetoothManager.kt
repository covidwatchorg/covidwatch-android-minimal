package org.covidwatch.android.ble

import android.app.Application
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEvent
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.ui.MainActivity
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothService
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothService.LocalBinder
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothServiceCallback
import org.tcncoalition.tcnclient.toBytes
import org.tcncoalition.tcnclient.toUUID
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.TimeUnit

interface BluetoothManager {
    fun stopAdvertiser()

    fun startService(tcn: ByteArray)
    fun stopService()

    fun changeAdvertisedValue(tcn: ByteArray)
}

class BluetoothManagerImpl(
    private val app: Application
) : BluetoothManager {

    private val intent get() = Intent(app, TcnBluetoothService::class.java)

    private var service: TcnBluetoothService? = null

    private var tcn: ByteArray? = null
    private var timer: Timer? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@BluetoothManagerImpl.service = (service as LocalBinder).service.apply {
                setTcnCallback(
                    object : TcnBluetoothServiceCallback {
                        override fun generateTcn() = tcn?: UUID.randomUUID().toBytes()

                        override fun onTcnFound(tcn: ByteArray) {
                            CovidWatchDatabase.databaseWriteExecutor.execute {
                                val dao: ContactEventDAO =
                                    CovidWatchDatabase.getInstance(app).contactEventDAO()
                                val contactEvent = ContactEvent(tcn.toUUID().toString())
                                val isCurrentUserSick = app.getSharedPreferences(
                                    app.getString(R.string.preference_file_key),
                                    Context.MODE_PRIVATE
                                ).getBoolean(
                                    app.getString(R.string.preference_is_current_user_sick),
                                    false
                                )
                                contactEvent.wasPotentiallyInfectious = isCurrentUserSick
                                dao.insert(contactEvent)
                            }
                        }
                    }
                )
                setForegroundNotification(foregroundNotification())
                startTcnExchange()
            }

            runTimer()
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    private fun runTimer() {
        // scheduler a new timer to start changing the contact event numbers
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    service?.updateCen()
                }
            },
            TimeUnit.MINUTES.toMillis(CEN_CHANGE_INTERVAL_MIN),
            TimeUnit.MINUTES.toMillis(CEN_CHANGE_INTERVAL_MIN)
        )
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

    override fun changeAdvertisedValue(tcn: ByteArray) {
        service?.updateCen()
    }

    override fun startService(tcn: ByteArray) {
        this.tcn = tcn
        app.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        app.startService(intent)
    }

    override fun stopAdvertiser() {
        service?.stopTcnExchange()
    }

    override fun stopService() {
        service?.stopTcnExchange()
        app.stopService(intent)
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

    companion object {
        // CONSTANTS
        private const val CEN_CHANGE_INTERVAL_MIN = 15L
        private const val CHANNEL_ID = "CovidBluetoothContactChannel"
    }
}
