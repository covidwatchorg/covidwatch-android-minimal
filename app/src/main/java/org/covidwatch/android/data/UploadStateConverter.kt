package org.covidwatch.android.data

import androidx.room.TypeConverter

class UploadStateConverter {

    @TypeConverter
    fun toUploadState(status: Int): SignedReport.UploadState? {
        return when (status) {
            SignedReport.UploadState.NOTUPLOADED.code -> {
                SignedReport.UploadState.NOTUPLOADED
            }
            SignedReport.UploadState.UPLOADING.code -> {
                SignedReport.UploadState.UPLOADING
            }
            SignedReport.UploadState.UPLOADED.code -> {
                SignedReport.UploadState.UPLOADED
            }
            else -> {
                throw IllegalArgumentException("Could not recognize status")
            }
        }
    }

    @TypeConverter
    fun toInteger(status: SignedReport.UploadState): Int? {
        return status.code
    }
}

