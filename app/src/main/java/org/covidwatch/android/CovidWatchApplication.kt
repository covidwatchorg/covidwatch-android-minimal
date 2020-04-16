package org.covidwatch.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.work.*
import cafe.cryptography.ed25519.Ed25519PrivateKey
import org.covidwatch.android.ble.BluetoothManagerImpl
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.SignedReport
import org.covidwatch.android.data.TemporaryContactNumber
import org.covidwatch.android.data.TemporaryContactNumberDAO
import org.covidwatch.android.data.firestore.SignedReportsDownloadWorker
import org.covidwatch.android.data.firestore.SignedReportsUploader
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothServiceCallback
import org.tcncoalition.tcnclient.crypto.*
import org.tcncoalition.tcnclient.toBytes
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class CovidWatchApplication : Application() {

    companion object {
        private const val TAG = "COVIDWatchApplication"
    }

    private lateinit var signedReportsUploader: SignedReportsUploader
    private lateinit var currentUserExposureNotifier: CurrentUserExposureNotifier

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

        currentUserExposureNotifier = CurrentUserExposureNotifier(this)
        currentUserExposureNotifier.startObserving()

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

        override fun generateTcn(): ByteArray {
            val tcn = currentTemporaryContactKey.temporaryContactNumber.bytes
            val newTemporaryContactKey = currentTemporaryContactKey.ratchet()
            // TODO: Handle case when this is null. Generate new rak and set it up...
            if (newTemporaryContactKey != null) {
                currentTemporaryContactKey = newTemporaryContactKey
            }
            return tcn
        }

        override fun onTcnFound(tcn: ByteArray) {
            logTcn(tcn)
        }
    }

    private fun logTcn(tcn: ByteArray) {
        CovidWatchDatabase.databaseWriteExecutor.execute {
            val temporaryContactNumberDAO: TemporaryContactNumberDAO =
                CovidWatchDatabase.getInstance(this).temporaryContactNumberDAO()
            val temporaryContactNumber = TemporaryContactNumber()
            temporaryContactNumber.bytes = tcn
            temporaryContactNumberDAO.insert(temporaryContactNumber)
        }
    }

    // Do not keep the report authorization key around in memory,
    // since it contains sensitive information.
    // Fetch it every time from our secure store (Keychain).
    // TODO: How to store this in the KeyStore?!?!?!111??!
    private fun reportAuthorizationKey(): ReportAuthorizationKey {
        val sharedPreferences = getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
        var rakBase64Encoded = sharedPreferences.getString(
            getString(R.string.preference_rak),
            ""
        )
        if (rakBase64Encoded.isNullOrEmpty()) {
            rakBase64Encoded = Base64.encodeToString(
                Ed25519PrivateKey.generate(SecureRandom()).toByteArray(),
                Base64.DEFAULT
            )
            with(sharedPreferences.edit()) {
                putString(
                    getString(R.string.preference_rak),
                    rakBase64Encoded
                )
                commit()
            }
        }
        val rak = Base64.decode(rakBase64Encoded, Base64.DEFAULT)
        val key = Ed25519PrivateKey.fromByteArray(rak)
        return ReportAuthorizationKey(key)
    }

    // It is safe to store the temporary contact key in the user defaults,
    // since it does not contain sensitive information.
    var currentTemporaryContactKey: TemporaryContactKey
        get() {
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            ).apply {
                val tckBytes = getString(
                    getString(R.string.preference_tck),
                    null
                )
                return if (tckBytes != null) {
                    TemporaryContactKey.fromByteArray(Base64.decode(tckBytes, Base64.DEFAULT))
                } else {
                    // If there isn't a temporary contact key in the UserDefaults,
                    // then use the initial temporary contact key.
                    this@CovidWatchApplication.reportAuthorizationKey().initialTemporaryContactKey
                }
            }
        }
        set(value) {
            with(
                getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                ).edit()
            ) {
                putString(
                    getString(R.string.preference_tck),
                    Base64.encodeToString(value?.toByteArray(), Base64.DEFAULT)
                )
                commit()
            }
        }

    fun generateAndUploadReport() {
        val endIndex = currentTemporaryContactKey.index.uShort.toInt()
        val minutesIn14Days = 60 * 24 * 7 * 2
        val periods = minutesIn14Days / 15
        val startIndex = max(0, endIndex - periods)

        val tcnSignedReport = reportAuthorizationKey().createReport(
            MemoType.CovidWatchV1,
            "Hello, World!".toByteArray(),
            startIndex.toUShort(),
            endIndex.toUShort()
        )

        // Create a new Signed Report with `uploadState` set to `.notUploaded` and store it in the local persistent store.
        // This will kick off an observer that watches for signed reports which were not uploaded and will upload it.
        val signedReport = SignedReport(tcnSignedReport)
        signedReport.isProcessed = true
        signedReport.uploadState = SignedReport.UploadState.NOTUPLOADED

        CovidWatchDatabase.databaseWriteExecutor.execute {
            val dao =
                CovidWatchDatabase.getInstance(this).signedReportDAO()
            dao.insert(signedReport)
        }
    }
}
