package org.literacybridge.androidtbloader.checkin;

import android.util.Log;

import org.literacybridge.androidtbloader.community.CommunityInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a location csv file, and returns a list of CommunityInfo.
 */

class LocationsCsvFile {
    private static final String TAG = LocationsCsvFile.class.getSimpleName();

    private File inputFile;

    LocationsCsvFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public Map<String, CommunityInfo> read() {
        Map<String, CommunityInfo> resultList = new HashMap<>();
        try (InputStream is = new FileInputStream(inputFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
                // Allow comments as another field.
                if (row.length < 4) {
                    Log.d(TAG, String.format("Invalid location line: %s", csvLine));
                    continue;
                }
                String community = row[0].toUpperCase();
                String project = row[1].toUpperCase();
                double latitude = Double.parseDouble(row[2]);
                double longitude = Double.parseDouble(row[3]);
                resultList.put(community, new CommunityInfo(community, project, latitude, longitude));
            }
        } catch (Exception e) {
            Log.d(TAG, String.format("Exception parsing csv file: %s", inputFile.getName()), e);
            // Ignore exception; continue without location information.
        }
        return resultList;
    }
}

