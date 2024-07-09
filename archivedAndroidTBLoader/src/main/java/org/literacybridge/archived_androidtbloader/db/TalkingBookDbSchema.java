package org.literacybridge.archived_androidtbloader.db;

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

