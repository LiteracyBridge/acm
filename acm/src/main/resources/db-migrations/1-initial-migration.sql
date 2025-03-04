CREATE TABLE "migrations"
(
    "id"        INTEGER NOT NULL UNIQUE,
    "timestamp" TEXT    NOT NULL,
    "name"      INTEGER NOT NULL UNIQUE,
    PRIMARY KEY ("id" AUTOINCREMENT)
);

CREATE TABLE "deployments"
(
    "id"                INTEGER NOT NULL UNIQUE,
    "name"              TEXT    NOT NULL UNIQUE,
    "deployment_number" INTEGER NOT NULL,
    "committed"         INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id" AUTOINCREMENT),
    UNIQUE ("name", "deployment_number")
);

CREATE TABLE "playlists"
(
    "id"            INTEGER NOT NULL UNIQUE,
    "title"         TEXT    NOT NULL UNIQUE,
    "deployment_id" INTEGER,
    "committed"     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id" AUTOINCREMENT),
    UNIQUE ("title", "deployment_id"),
    FOREIGN KEY ("deployment_id") REFERENCES "deployments" ("id") ON DELETE CASCADE
);

CREATE TABLE "audio_items"
(
    "id"                    INTEGER NOT NULL UNIQUE,    -- random unique uuid
    "title"                 TEXT    NOT NULL,
    "playlist_id"           INTEGER,
    "language"              TEXT    NOT NULL,
    "duration"              TEXT,
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
    "acm_id"                TEXT,
    "related_id"            TEXT,
    "transcription"         TEXT,
    "note"                  TEXT,
    "beneficiary"           TEXT,
    "committed"             INTEGER NOT NULL DEFAULT 0, -- 0 = not committed, 1 = committed
    "type"                  TEXT,                       -- "SystemPrompt" or "Message" or "PlaylistPrompt"
    "source"                TEXT,
    PRIMARY KEY ("id" AUTOINCREMENT),
    UNIQUE ("playlist_id", "title", "language"),
    FOREIGN KEY ("playlist_id") REFERENCES "playlists" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("related_id") REFERENCES "audio_items" ("acm_id") ON DELETE SET NULL
);

CREATE TABLE "packages"
(
    "id"            INTEGER NOT NULL UNIQUE,
    "name"          TEXT    NOT NULL,
    "platform"      TEXT    NOT NULL DEFAULT 'TalkingBook', -- TalkingBook or CompanionApp
    "created_at"    TEXT    NOT NULL,
    "published"     INTEGER NOT NULL,                       -- 0 = not published, 1 = published
    "deployment_id" INTEGER NOT NULL,
    PRIMARY KEY ("id" AUTOINCREMENT),
    FOREIGN KEY ("deployment_id") REFERENCES "deployments" ("id") ON DELETE CASCADE
);

CREATE TABLE "package_contents"
(
    "id"         INTEGER NOT NULL UNIQUE,
    "package_id" INTEGER NOT NULL,
    "path"       TEXT    NOT NULL,
    "ref_id"     INTEGER,
    PRIMARY KEY ("id" AUTOINCREMENT),
    FOREIGN KEY ("package_id") REFERENCES "packages" ("id") ON DELETE CASCADE,
    FOREIGN KEY ("ref_id") REFERENCES "audio_items" ("id")
);