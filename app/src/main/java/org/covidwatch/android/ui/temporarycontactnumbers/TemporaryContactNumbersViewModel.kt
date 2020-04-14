package org.covidwatch.android.ui.temporarycontactnumbers

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import org.covidwatch.android.R
import org.covidwatch.android.data.TemporaryContactNumber
import org.covidwatch.android.data.TemporaryContactNumberDAO

class TemporaryContactNumbersViewModel(temporaryContactNumberDAO: TemporaryContactNumberDAO, application: Application) : AndroidViewModel(application) {

    val temporaryContactEvents: LiveData<PagedList<TemporaryContactNumber>> =
        temporaryContactNumberDAO.pagedAllSortedByDescTimestamp.toLiveData(pageSize = 50)

    var isContactEventLoggingEnabled = MutableLiveData<Boolean>().apply {
        val isEnabled = application.getSharedPreferences(
            application.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).getBoolean(
            application.getString(R.string.preference_is_temporary_contact_number_logging_enabled),
            false
        )
        value = isEnabled
    }

}