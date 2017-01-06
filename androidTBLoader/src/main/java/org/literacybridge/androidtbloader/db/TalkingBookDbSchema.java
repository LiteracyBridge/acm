package org.literacybridge.androidtbloader.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class TalkingBookDbSchema {
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

}

