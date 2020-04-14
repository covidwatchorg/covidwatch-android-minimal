package org.covidwatch.android.data.firestore

import android.app.Application
import android.util.Log
import androidx.lifecycle.Observer
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.covidwatch.android.data.TemporaryContactNumber
import org.covidwatch.android.data.CovidWatchDatabase

class LocalContactEventsUploader(var application: Application) {

    private val viewModel: LocalContactEventsViewModel = LocalContactEventsViewModel(
        CovidWatchDatabase.getInstance(application).tempraryContactNumberDAO(),
        application
    )

    private val tempraryContactNumberDAO =
        CovidWatchDatabase.getInstance(application).tempraryContactNumberDAO()

    fun startUploading() {
//        viewModel.temporaryContactEvents.observeForever(Observer {
//            uploadContactEventsIfNeeded(it)
//        })
    }

//    private fun uploadContactEventsIfNeeded(temporaryContactNumbers: List<TemporaryContactNumber>) {
//        val temporaryContactNumbersToUpload = temporaryContactNumbers.filter {
//            it.wasPotentiallyInfectious && it.uploadState == TemporaryContactNumber.UploadState.NOTUPLOADED
//        }
//        uploadContactEvents(temporaryContactNumbersToUpload)
//    }
//
//    private fun uploadContactEvents(temporaryContactNumbers: List<TemporaryContactNumber>) {
//        if (temporaryContactNumbers.isEmpty()) return
//        Log.i(TAG, "Uploading ${temporaryContactNumbers.size} temporary contact number(s)...")
//        CovidWatchDatabase.databaseWriteExecutor.execute {
//            temporaryContactNumbers.forEach { it.uploadState = TemporaryContactNumber.UploadState.UPLOADING }
//            tempraryContactNumberDAO.update(temporaryContactNumbers)
//        }
//        val db = FirebaseFirestore.getInstance()
//        db.runBatch { batch ->
//            temporaryContactNumbers.forEach {
////                batch.set(
////                    db.collection(FirestoreConstants.COLLECTION_TEMPORARY_CONTACT_NUMBERS)
////                        .document(it.bytes),
////                    hashMapOf(FirestoreConstants.FIELD_TIMESTAMP to Timestamp(it.foundDate))
////                )
//            }
//        }.addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                Log.i(TAG, "Uploaded ${temporaryContactNumbers.size} temporary contact number(s)")
//                CovidWatchDatabase.databaseWriteExecutor.execute {
//                    temporaryContactNumbers.forEach { it.uploadState = TemporaryContactNumber.UploadState.UPLOADED }
//                    tempraryContactNumberDAO.update(temporaryContactNumbers)
//                }
//            } else {
//                Log.d(
//                    TAG,
//                    "Uploading ${temporaryContactNumbers.size} temporary contact number(s) failed: ${task.exception}"
//                )
//                CovidWatchDatabase.databaseWriteExecutor.execute {
//                    temporaryContactNumbers.forEach { it.uploadState = TemporaryContactNumber.UploadState.NOTUPLOADED }
//                    tempraryContactNumberDAO.update(temporaryContactNumbers)
//                }
//            }
//        }
//    }

    companion object {
        private const val TAG = "LocalContactEventsUploader"
    }

}
