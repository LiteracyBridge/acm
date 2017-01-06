package org.literacybridge.androidtbloader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import org.literacybridge.androidtbloader.db.TalkingBookDbSchema.DeploymentPackagesTable;
import org.literacybridge.androidtbloader.db.TalkingBookDbSchema.KnownTalkingBooksTable;

public class TalkingBookDbHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "talking_books.db";

    public TalkingBookDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + KnownTalkingBooksTable.NAME + "( "
                + "_id integer primary key autoincrement, "
                + KnownTalkingBooksTable.Cols.USB_UUID + ", "
                + KnownTalkingBooksTable.Cols.BASE_URI + ")");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
