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
import android.content.Context;
import android.net.Uri;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

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
                "KB", "MB", "GB", "TB"
        };
        double sizeNum = bytes;
        for (int i = 0;; i++) {
            if (i >= quantifiers.length) {
                return "Too Much";
            }
            sizeNum /= 1024;
            if (sizeNum < 512) {
                return String.format("%.2f %s", sizeNum, quantifiers[i]);
            }
        }
    }

    /**
     * Copies the data from the passed in Uri, to a new file for use with the
     * Transfer Service
     * 
     * @param context
     * @param uri
     * @return
     * @throws IOException
     */
    public static File copyContentUriToFile(Context context, Uri uri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        File copiedData = new File(context.getDir("SampleImagesDir", Context.MODE_PRIVATE), UUID
                .randomUUID().toString());
        copiedData.createNewFile();

        FileOutputStream fos = new FileOutputStream(copiedData);
        byte[] buf = new byte[2046];
        int read = -1;
        while ((read = is.read(buf)) != -1) {
            fos.write(buf, 0, read);
        }

        fos.flush();
        fos.close();

        return copiedData;
    }


}
