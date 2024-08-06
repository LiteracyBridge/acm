package org.literacybridge.tbloaderandroid.util

import androidx.compose.ui.unit.dp
import org.literacybridge.tbloaderandroid.BuildConfig

class Constants {
    companion object {
        const val LOG_TAG = "TalkingBook";

        val API_URL: String = BuildConfig.API_URL
        const val SRN_HELPER_URL = "https://lj82ei7mce.execute-api.us-west-2.amazonaws.com/Prod"
        const val DEPLOYMENTS_BUCKET_NAME = "acm-content-updates"

        // Smart Sync programs are hosted here, and their deployments come from here as well.
        const val CONTENT_BUCKET_NAME = "amplio-program-content"

        /**
         * Statistics, logs get uploaded to here.
         */
        const val COLLECTED_DATA_BUCKET_NAME = "acm-stats"

        /**
         * Collected data from talking books are uploaded to this folder/object
         * in the 'amplio-program-content' bucket. A lambda function automatically moves the files into
         * acm-stats buckets.
         * Amplify storage supports only one bucket, as a result, collected data cannot be uploaded to
         * 'acm-stats' bucket because 'amplio-program-content' is configured as the default bucket.
         */
        const val COLLECTED_DATA_DIR_NAME = "staging-android-collected-data"

        // Time to sit twiddling thumbs on Android, because we can't flush files on USB storage.
        const val AndroidPostUpdateSleepTime = 5000 // millis

        val SCREEN_MARGIN = 5.dp
    }
}
