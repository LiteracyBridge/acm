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

    /**
     * Converts number of bytes into proper scale.
     *
     * @param bytes number of bytes to be converted.
     * @return A string that represents the bytes in a proper scale.
     */
    @SuppressLint("DefaultLocale")
    public static String getBytesString(long bytes) {
        String[] quantifiers = new String[] {
                "KiB", "MiB", "GiB", "TiB"
        };
        double sizeNum = bytes;
        for (int i = 0;; i++) {
            if (i >= quantifiers.length) {
                return "Too Much";
            }
            sizeNum /= 1024;
            if (sizeNum <= 999) {
                return String.format("%.2f %s", sizeNum, quantifiers[i]);
            }
        }
    }

    public static String join(Collection toJoin, String with) {
        StringBuilder result = new StringBuilder();
        for (Object o : toJoin) {
            if (result.length() > 0) { result.append(with); }
            result.append(o.toString());
        }
        return result.toString();
    }

}
