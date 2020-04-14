package org.covidwatch.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

//  Created by Zsombor SZABO on 14/04/2020.

@Entity(tableName = "signed_reports")
@TypeConverters(DateConverter::class, UploadStateConverter::class)
class SignedReport {

    @ColumnInfo(name = "end_index")
    var endIndex: Int = 0

    @ColumnInfo(name = "is_processed")
    var isProcessed: Boolean = false

    @ColumnInfo(name = "memo_data", typeAffinity = ColumnInfo.BLOB)
    var memoData: ByteArray = ByteArray(0)

    @ColumnInfo(name = "memo_type")
    var memoType: Int = 0

    @ColumnInfo(name = "report_verification_public_key_bytes", typeAffinity = ColumnInfo.BLOB)
    var reportVerificationPublicKeyBytes: ByteArray = ByteArray(0)

    @PrimaryKey
    @ColumnInfo(name = "signature_bytes", typeAffinity = ColumnInfo.BLOB)
    var signatureBytes: ByteArray = ByteArray(0)

    @ColumnInfo(name = "start_index")
    var startIndex: Int = 0

    @ColumnInfo(name = "temporary_contact_key_bytes", typeAffinity = ColumnInfo.BLOB)
    var temporaryContactKeyBytes: ByteArray = ByteArray(0)

    @ColumnInfo(name = "upload_state")
    var uploadState: UploadState = UploadState.NOTUPLOADED

    enum class UploadState(val code: Int) {
        NOTUPLOADED(0), UPLOADING(1), UPLOADED(2);
    }
}
