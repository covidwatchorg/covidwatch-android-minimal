package org.covidwatch.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity(tableName = "temporary_contact_numbers")
@TypeConverters(DateConverter::class)
class TemporaryContactNumber {

    @PrimaryKey
    @ColumnInfo(name = "bytes", typeAffinity = ColumnInfo.BLOB)
    var bytes: ByteArray = ByteArray(0)

    @ColumnInfo(name = "foundDate")
    var foundDate: Date = Date()

    @ColumnInfo(name = "was_potentially_infectious")
    var wasPotentiallyInfectious: Boolean = false
}