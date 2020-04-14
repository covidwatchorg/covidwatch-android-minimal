package org.covidwatch.android.data.firestore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.covidwatch.android.data.TemporaryContactNumber
import org.covidwatch.android.data.TemporaryContactNumberDAO

class LocalContactEventsViewModel(
    temporaryContactNumberDAO: TemporaryContactNumberDAO,
    application: Application
) : AndroidViewModel(application) {

    val temporaryContactEvents: LiveData<List<TemporaryContactNumber>> = temporaryContactNumberDAO.allSortedByDescTimestamp
}
