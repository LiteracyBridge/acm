package org.literacybridge.talkingbookapp.util

import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer


/*
* Handles basic helper functions used throughout the app.
*/
object Util {
    fun join(toJoin: Collection<*>, with: String?): String {
        val result = StringBuilder()
        for (o in toJoin) {
            if (result.isNotEmpty()) {
                result.append(with)
            }
            result.append(o.toString())
        }
        return result.toString()
    }

    fun formatElapsedTime(millis: Long): String {
        return if (millis < 1000) {
            // Less than one second
            String.format("%d ms", millis)
        } else if (millis < 60000) {
            // Less than one minute. Format like '1.25 s' or '25.3 s' (3 digits).
            val time = String.format("%f", millis / 1000.0)
            time.substring(0, 4) + " s"
        } else {
            val minutes = millis / 60000
            val seconds = millis % 60000 / 1000
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun getStackTrace(aThrowable: Throwable): String {
        val result: Writer = StringWriter()
        val printWriter: PrintWriter = PrintWriter(result)
        aThrowable.printStackTrace(printWriter)
        return result.toString()
    }
}

