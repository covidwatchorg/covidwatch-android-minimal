package org.covidwatch.android.ui.temporarycontactnumbers

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.covidwatch.android.data.TemporaryContactNumberDAO

class TemporaryContactNumbersViewModelFactory(
    private val temporaryContactNumberDAO: TemporaryContactNumberDAO,
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TemporaryContactNumbersViewModel(temporaryContactNumberDAO, application) as T
    }
}
