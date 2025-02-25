CREATE TABLE "migrations"
(
    "id"         INTEGER NOT NULL UNIQUE,
    "timestamp" TEXT    NOT NULL,
    "name"       INTEGER NOT NULL UNIQUE,
    PRIMARY KEY ("id" AUTOINCREMENT)
);

CREATE TABLE "deployments"
(
    "id"                TEXT    NOT NULL UNIQUE,
    "name"              TEXT    NOT NULL UNIQUE,
    "deployment_number" INTEGER NOT NULL UNIQUE,
    "committed"         INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

CREATE TABLE "playlist"
(
    "id"            TEXT    NOT NULL UNIQUE,
    "titlle"        TEXT,
    "deployment_id" TEXT,
    "committed"     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id"),
    FOREIGN KEY ("deployment_id") REFERENCES "deployments" ("id") ON DELETE CASCADE

);

CREATE TABLE "audio_items"
(
    "id"                    TEXT    NOT NULL UNIQUE,
    "title"                 TEXT    NOT NULL,
    "playlist_id"           TEXT,
    "language"              TEXT    NOT NULL,
    "duration"              INTEGER,
    "file_path"             TEXT,
    "position"              INTEGER,
    "format"                TEXT,
    "default_category_code" TEXT,
    "variant"               TEXT,
    "sdg_goal_id"           INTEGER,
    "sdg_target"            TEXT,
    "key_points"            TEXT,
    "sdg_target_id"         TEXT,
    "category"              TEXT,
    "created_at"            TEXT,
    "status"                TEXT,
    "volume"                INTEGER          DEFAULT 100,
    "keywords"              TEXT,
    "timing"                TEXT,
    "primary_speaker"       TEXT,
    "device_id"             TEXT,
    "message_id"            TEXT,
    "related_message_id"    TEXT,
    "transcription"         TEXT,
    "note"                  TEXT,
    "beneficiary"           TEXT,
    "committed"             INTEGER NOT NULL DEFAULT 0, -- 0 = not committed, 1 = committed
    "type"                  TEXT,                       -- "SystemPrompt" or "Message" or "PlaylistPrompt"
    "source"                TEXT,                       -- File path to file, if copied from a template
    PRIMARY KEY ("id"),
    UNIQUE ("playlist_id", "title"),
    FOREIGN KEY ("playlist_id") REFERENCES "" ON DELETE CASCADE,
    FOREIGN KEY ("related_message_id") REFERENCES "audio_items" ("message_id") ON DELETE SET NULL
);

CREATE TABLE "packages"
(
    "id"            INTEGER NOT NULL UNIQUE,
    "name"          TEXT    NOT NULL,
    "platform"      TEXT    NOT NULL DEFAULT 'TalkingBook', -- TalkingBook or CompanionApp
    "created_at"    TEXT    NOT NULL,
    "published"     INTEGER NOT NULL,                       -- 0 = not published, 1 = published
    "deployment_id" TEXT    NOT NULL,
    PRIMARY KEY ("id" AUTOINCREMENT)
);

CREATE TABLE "package_contents"
(
    "id"         TEXT    NOT NULL UNIQUE,
    "package_id" INTEGER NOT NULL,
    "path"       TEXT    NOT NULL,
    "ref_id"     TEXT,
    PRIMARY KEY ("id"),
    FOREIGN KEY ("package_id") REFERENCES "packages" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("ref_id") REFERENCES "audio_items" ("id")
);