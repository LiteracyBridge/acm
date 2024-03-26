package org.literacybridge.talkingbookapp.util

import androidx.compose.ui.unit.dp

class Constants {
    companion object {
        const val LOG_TAG = "TalkingBook";

        const val API_URL = "https://nhr12r5plj.execute-api.us-west-2.amazonaws.com/dev/"
        const val SRN_HELPER_URL = "https://lj82ei7mce.execute-api.us-west-2.amazonaws.com/Prod"
        const val DEPLOYMENTS_BUCKET_NAME = "acm-content-updates"

        // Smart Sync programs are hosted here, and their deployments come from here as well.
        const val CONTENT_BUCKET_NAME = "amplio-program-content"

        // Statistics, logs get uploaded to here.
        const val COLLECTED_DATA_BUCKET_NAME = "acm-stats"

        // Time to sit twiddling thumbs on Android, because we can't flush files on USB storage.
        const val AndroidPostUpdateSleepTime = 5000 // millis


        val SCREEN_MARGIN = 1.dp
    }
}