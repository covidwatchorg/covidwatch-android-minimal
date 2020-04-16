package org.covidwatch.android.data

import androidx.lifecycle.LiveData
import androidx.room.*

//  Created by Zsombor SZABO on 15/04/2020.

@Dao
interface SignedReportDAO {

    @get:Query("SELECT * FROM signed_reports")
    val all: LiveData<List<SignedReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(signedReport: SignedReport)

    @Update
    fun update(signedReport: SignedReport)

    @Update
    fun update(signedReport: List<SignedReport>)

}
