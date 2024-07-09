package org.literacybridge.androidtbloader.util

import android.icu.text.SimpleDateFormat
import android.net.ParseException
import android.util.Log
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class TimeAgo2 {
    fun toText(dataDate: String?): String? {
        var convTime: String? = null
        val suffix = "Ago"

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val pasTime: Date = dateFormat.parse(dataDate)
            val nowTime = Date()
            val dateDiff: Long = nowTime.time - pasTime.time
            val second: Long = TimeUnit.MILLISECONDS.toSeconds(dateDiff)
            val minute: Long = TimeUnit.MILLISECONDS.toMinutes(dateDiff)
            val hour: Long = TimeUnit.MILLISECONDS.toHours(dateDiff)
            val day: Long = TimeUnit.MILLISECONDS.toDays(dateDiff)
            if (second < 60) {
                convTime = "$second Seconds $suffix"
            } else if (minute < 60) {
                convTime = "$minute Minutes $suffix"
            } else if (hour < 24) {
                convTime = "$hour Hours $suffix"
            } else if (day >= 7) {
                convTime = if (day > 360) {
                    (day / 360).toString() + " Years " + suffix
                } else if (day > 30) {
                    (day / 30).toString() + " Months " + suffix
                } else {
                    (day / 7).toString() + " Week " + suffix
                }
            } else if (day < 7) {
                convTime = "$day Days $suffix"
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            e.localizedMessage?.let { Log.e("ConvTimeE", it) }
        }

        return convTime
    }
}