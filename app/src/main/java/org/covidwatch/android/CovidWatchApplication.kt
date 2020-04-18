package org.covidwatch.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.covidwatch.android.ble.BluetoothManagerImpl
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.SignedReport
import org.covidwatch.android.data.TemporaryContactNumber
import org.covidwatch.android.data.TemporaryContactNumberDAO
import org.covidwatch.android.data.firestore.SignedReportsDownloadWorker
import org.covidwatch.android.data.firestore.SignedReportsUploader
import org.tcncoalition.tcnclient.TcnKeys
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothServiceCallback
import java.util.*
import java.util.concurrent.TimeUnit

class CovidWatchApplication : Application() {

    private val tcnKeys = TcnKeys(this)
    private lateinit var signedReportsUploader: SignedReportsUploader

    //TODO: Move to DI module
    private var bluetoothManager: BluetoothManagerImpl? = null

    private var sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                getString(R.string.preference_is_temporary_contact_number_logging_enabled) -> {
                    val isContactEventLoggingEnabled = sharedPreferences.getBoolean(
                        getString(R.string.preference_is_temporary_contact_number_logging_enabled),
                        true
                    )
                    configureContactTracing(isContactEventLoggingEnabled)
                }
            }
        }

    private fun configureContactTracing(enabled: Boolean) {
        if (enabled) {
            bluetoothManager?.startService()
        } else {
            bluetoothManager?.stopService()
        }
    }

    override fun onCreate() {
        super.onCreate()

        getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        bluetoothManager = BluetoothManagerImpl(this, tcnBluetoothServiceCallback)

        signedReportsUploader = SignedReportsUploader(this)
        signedReportsUploader.startUploading()

        schedulePeriodicRefresh()

        val isContactEventLoggingEnabled = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ).getBoolean(
            getString(R.string.preference_is_temporary_contact_number_logging_enabled),
            true
        )
        configureContactTracing(isContactEventLoggingEnabled)
    }

    fun refreshOneTime() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest =
            OneTimeWorkRequestBuilder<SignedReportsDownloadWorker>()
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            SignedReportsDownloadWorker.WORKER_NAME,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    private fun schedulePeriodicRefresh() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest =
            PeriodicWorkRequestBuilder<SignedReportsDownloadWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SignedReportsDownloadWorker.WORKER_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    /* TCN */

    private val tcnBluetoothServiceCallback = object : TcnBluetoothServiceCallback {
        override fun generateTcn() = tcnKeys.generateTcn()

        override fun onTcnFound(tcn: ByteArray, estimatedDistance: Double?) = logTcn(tcn, estimatedDistance)
    }

    private fun logTcn(tcn: ByteArray, estimatedDistance: Double?) {
        CovidWatchDatabase.databaseWriteExecutor.execute {

            val temporaryContactNumberDAO: TemporaryContactNumberDAO =
                CovidWatchDatabase.getInstance(this).temporaryContactNumberDAO()

            val temporaryContactNumber = temporaryContactNumberDAO.findByPrimaryKey(tcn)
            if (temporaryContactNumber == null) {
                val temporaryContactNumber = TemporaryContactNumber()
                temporaryContactNumber.bytes = tcn
                if (estimatedDistance != null && estimatedDistance < temporaryContactNumber.closestEstimatedDistanceMeters) {
                    temporaryContactNumber.closestEstimatedDistanceMeters = estimatedDistance
                }
                temporaryContactNumberDAO.insert(temporaryContactNumber)
            }
            else {
                temporaryContactNumber.lastSeenDate = Date()
                if (estimatedDistance != null && estimatedDistance < temporaryContactNumber.closestEstimatedDistanceMeters) {
                    temporaryContactNumber.closestEstimatedDistanceMeters = estimatedDistance
                }
                temporaryContactNumberDAO.update(temporaryContactNumber)
            }
        }
    }

    fun generateAndUploadReport() {
        // Create a new Signed Report with `uploadState` set to `.notUploaded` and store it in the local persistent store.
        // This will kick off an observer that watches for signed reports which were not uploaded and will upload it.
        val signedReport = SignedReport(tcnKeys.createReport())
        signedReport.isProcessed = true
        signedReport.uploadState = SignedReport.UploadState.NOTUPLOADED

        CovidWatchDatabase.databaseWriteExecutor.execute {
            val dao =
                CovidWatchDatabase.getInstance(this).signedReportDAO()
            dao.insert(signedReport)
        }
    }
}
