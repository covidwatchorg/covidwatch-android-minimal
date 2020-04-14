package org.covidwatch.android.ui.contactevents.adapters

import android.text.format.DateUtils
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.databinding.BindingAdapter
import com.google.android.gms.common.util.Base64Utils
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
            result = Base64Utils.encode(tcn)
        }
        textView.text = result
    }
}
