package org.literacybridge.androidtbloader.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWrapper;

import org.literacybridge.androidtbloader.DeploymentPackage;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class TalkingBookDBSchema {
    public static final class KnownTalkingBooksTable {
        public static final String NAME = "known_talking_books";

        public static final class Cols {
            public static final String USB_UUID = "usb_uuid";
            public static final String BASE_URI = "base_uri";
        }
    }

    public static final class DeploymentPackagesTable {
        public static final String NAME = "deployment_packages";

        public static final class Cols {
            public static final String PROJECT_NAME = "project_name";
            public static final String REVISION = "rev";
            public static final String EXPIRATION = "expiration";
            public static final String COMMUNITIES = "communities";
        }
    }

    public static final class DeploymentPackagesCursorWrapper extends CursorWrapper {
        public DeploymentPackagesCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        public DeploymentPackage getDeploymentPackage() {
            String projectName = getString(getColumnIndex(DeploymentPackagesTable.Cols.PROJECT_NAME));
            String revision = getString(getColumnIndex(DeploymentPackagesTable.Cols.REVISION));
            long expiration = getLong(getColumnIndex(DeploymentPackagesTable.Cols.EXPIRATION));
            String communitiesString = getString(getColumnIndex(DeploymentPackagesTable.Cols.COMMUNITIES));

            Set<String> communities = new HashSet<>();
            StringTokenizer tokenizer = new StringTokenizer(communitiesString, ";");
            while (tokenizer.hasMoreTokens()) {
                communities.add(tokenizer.nextToken());
            }

            return new DeploymentPackage(projectName, revision, new Date(expiration),
                    communities, DeploymentPackage.DownloadStatus.DOWNLOADED);
        }
    }

    public static ContentValues getContentValues(DeploymentPackage deploymentPackage) {
        ContentValues values = new ContentValues();
        values.put(DeploymentPackagesTable.Cols.PROJECT_NAME, deploymentPackage.getProjectName());
        values.put(DeploymentPackagesTable.Cols.REVISION, deploymentPackage.getRevision());
        if (deploymentPackage.getExpiration() != null) {
            values.put(DeploymentPackagesTable.Cols.EXPIRATION, deploymentPackage.getExpiration().getTime());
        }

        StringBuilder builder = new StringBuilder();
        for (String s : deploymentPackage.getCommunitiesFilter()) {
            builder.append(s).append(";");
        }
        values.put(DeploymentPackagesTable.Cols.COMMUNITIES, builder.toString());

        return values;
    }
}
