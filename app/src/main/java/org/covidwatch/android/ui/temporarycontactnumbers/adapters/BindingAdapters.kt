package org.covidwatch.android.ui.temporarycontactnumbers.adapters

import android.text.format.DateUtils
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.databinding.BindingAdapter
import android.util.Base64
import java.util.*

//  Created by Zsombor SZABO on 14/04/2020.

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("bindRelativeDate")
    fun bindRelativeDate(@NonNull textView: TextView, date: Date?) {
        var formattedDate: CharSequence = ""
        if (date != null) {
            formattedDate = DateUtils.getRelativeDateTimeString(
                textView.context,
                date.time,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_SHOW_YEAR
            );
        }
        textView.text = formattedDate
    }

    @JvmStatic
    @BindingAdapter("bindTCNBytes")
    fun bindTCNBytes(@NonNull textView: TextView, tcn: ByteArray?) {
        var result: CharSequence = ""
        if (tcn != null) {
            result = Base64.encodeToString(tcn, Base64.NO_WRAP)
        }
        textView.text = result
    }
}
