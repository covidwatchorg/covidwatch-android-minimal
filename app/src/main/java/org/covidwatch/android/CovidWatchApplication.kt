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
import okhttp3.OkHttpClient
import org.covidwatch.android.data.firestore.SignedReportsDownloadWorker
import org.covidwatch.android.data.firestore.SignedReportsUploader
import org.tcncoalition.tcnclient.TcnClient
import org.tcncoalition.tcnclient.TcnKeys
import java.util.concurrent.TimeUnit

class CovidWatchApplication : Application() {

    private lateinit var signedReportsUploader: SignedReportsUploader

    private var sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                getString(R.string.preference_is_temporary_contact_number_logging_enabled) -> {
                    configureContactTracing()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        TcnClient.init(
            CovidWatchTcnManager(
                this,
                TcnKeys(this)
            )
        )
        getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        // TODO: OkHttpClients Should Be Shared. Declare it as singleton in DI
        val okHttpClient = OkHttpClient()
        signedReportsUploader = SignedReportsUploader(this, okHttpClient)
        signedReportsUploader.startUploading()

        configureContactTracing()

        schedulePeriodicRefresh()
    }

    private fun configureContactTracing() {
        val isContactEventLoggingEnabled = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ).getBoolean(
            getString(R.string.preference_is_temporary_contact_number_logging_enabled),
            false
        )
        configureContactTracing(isContactEventLoggingEnabled)
    }

    private fun configureContactTracing(enabled: Boolean) {
        if (enabled) {
            TcnClient.tcnManager?.startService()
        } else {
            TcnClient.tcnManager?.stopService()
        }
    }

    fun scheduleRefreshOneTime() {
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
}
