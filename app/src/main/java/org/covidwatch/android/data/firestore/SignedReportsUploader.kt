package org.covidwatch.android.data.firestore

import android.app.Application
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.SignedReport
import java.util.*

class SignedReportsUploader(var application: Application) {

    companion object {
        private const val TAG = "SignedReportsUploader"
    }

    private val viewModel: SignedReportViewModel = SignedReportViewModel(
        CovidWatchDatabase.getInstance(application).signedReportDAO(),
        application
    )

    private val signedReportDAO =
        CovidWatchDatabase.getInstance(application).signedReportDAO()

    fun startUploading() {
        viewModel.signedReports.observeForever {
            uploadSignedReportsNeeded(it)
        }
    }

    private fun uploadSignedReportsNeeded(signedReports: List<SignedReport>) {
        val signedReportsToUpload = signedReports.filter {
            it.uploadState == SignedReport.UploadState.NOTUPLOADED
        }
        uploadContactEvents(signedReportsToUpload)
    }

    private fun uploadContactEvents(signedReports: List<SignedReport>) {
        if (signedReports.isEmpty()) return

        val db = FirebaseFirestore.getInstance()

        CovidWatchDatabase.databaseWriteExecutor.execute {
            signedReports.forEach { signedReport ->

                val signatureBytesBase64EncodedString =
                    Base64.encodeToString(signedReport.signatureBytes, Base64.NO_WRAP)

                Log.i(TAG, "Uploading signed report ($signatureBytesBase64EncodedString)...")

                signedReport.uploadState = SignedReport.UploadState.UPLOADING
                signedReportDAO.update(signedReport)


                val data = hashMapOf(
                    FirestoreConstants.FIELD_TEMPORARY_CONTACT_KEY_BYTES to Blob.fromBytes(signedReport.temporaryContactKeyBytes),
                    FirestoreConstants.FIELD_END_INDEX to signedReport.endIndex,
                    FirestoreConstants.FIELD_MEMO_DATA to Blob.fromBytes(signedReport.memoData),
                    FirestoreConstants.FIELD_MEMO_TYPE to signedReport.memoType,
                    FirestoreConstants.FIELD_REPORT_VERIFICATION_PUBLIC_KEY_BYTES to Blob.fromBytes(signedReport.reportVerificationPublicKeyBytes),
                    FirestoreConstants.FIELD_SIGNATURE_BYTES to Blob.fromBytes(signedReport.signatureBytes),
                    FirestoreConstants.FIELD_START_INDEX to signedReport.startIndex,
                    FirestoreConstants.FIELD_TIMESTAMP to FieldValue.serverTimestamp()
                )
                db.collection(FirestoreConstants.COLLECTION_SIGNED_REPORTS).add(data)
                    .addOnSuccessListener { _ ->
                        Log.i(TAG, "Uploaded signed report ($signatureBytesBase64EncodedString)")
                        signedReport.uploadState = SignedReport.UploadState.UPLOADED
                        CovidWatchDatabase.databaseWriteExecutor.execute {
                            signedReportDAO.update(signedReport)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            TAG,
                            "Uploading signed report ($signatureBytesBase64EncodedString) failed: $e"
                        )
                        signedReport.uploadState = SignedReport.UploadState.NOTUPLOADED
                        CovidWatchDatabase.databaseWriteExecutor.execute {
                            signedReportDAO.update(signedReport)
                        }
                    }
            }
        }
    }

}
