package org.clubelec.clubelecemailkiosk.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_EMAILS = "emails";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_EMAIL = "email";
    private static final String DATABASE_NAME = "email.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_TABLE_EMAILS =
            "CREATE TABLE " + TABLE_EMAILS + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_EMAIL + " TEXT NOT NULL" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_EMAILS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMAILS);
        onCreate(db);
    }
}

