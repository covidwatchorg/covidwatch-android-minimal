package org.covidwatch.android.data.firestore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.covidwatch.android.data.SignedReport
import org.covidwatch.android.data.SignedReportDAO

//  Created by Zsombor SZABO on 15/04/2020.

class SignedReportViewModel(
    signedReportDAO: SignedReportDAO,
    application: Application
) : AndroidViewModel(application) {

    val signedReports: LiveData<List<SignedReport>> = signedReportDAO.all
}