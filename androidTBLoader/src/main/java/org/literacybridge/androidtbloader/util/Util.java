/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.literacybridge.androidtbloader.util;

import android.annotation.SuppressLint;

import java.util.Collection;

/*
 * Handles basic helper functions used throughout the app.
 */
public class Util {

    public static String join(Collection toJoin, String with) {
        StringBuilder result = new StringBuilder();
        for (Object o : toJoin) {
            if (result.length() > 0) { result.append(with); }
            result.append(o.toString());
        }
        return result.toString();
    }

    @SuppressLint("DefaultLocale")
    public static String formatElapsedTime(Long millis) {
        if (millis < 1000) {
            // Less than one second
            return String.format("%d ms", millis);
        } else if (millis < 60000) {
            // Less than one minute. Format like '1.25 s' or '25.3 s' (3 digits).
            String time = String.format("%f", millis / 1000.0);
            return time.substring(0, 4) + " s";
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
