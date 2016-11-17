package com.group3.synthesizerapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.group3.synthesizerapp.verticalSeekBar.CustomSeekBar;

import java.util.ArrayList;

/**
 * Created by Marvin on 11/11/2016.
 */
public class PresetDataHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PresetData.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PresetDataContract.PresetEntry.TABLE_NAME
                            + " (" + PresetDataContract.PresetEntry.PRESET_NAME + " TEXT,"
                            + PresetDataContract.PresetEntry.PRESET_VALUE + " REAL"+ " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PresetDataContract.PresetEntry.TABLE_NAME;

    public PresetDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void storePresetValue(String presetName, float [] value)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        db.beginTransaction();
        for(int i = 0; i < 52 ; i++)
        {
            contentValues.put(PresetDataContract.PresetEntry.PRESET_NAME,presetName);
            contentValues.put(PresetDataContract.PresetEntry.PRESET_VALUE,value[i]);
            db.insert(PresetDataContract.PresetEntry.TABLE_NAME, null, contentValues);
            contentValues.remove(PresetDataContract.PresetEntry.PRESET_NAME);
            contentValues.remove(PresetDataContract.PresetEntry.PRESET_VALUE);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }


    public ArrayList<String> getPresetNames()
    {
        ArrayList<String> list = new ArrayList<String>();

        SQLiteDatabase db = this.getReadableDatabase();
        String [] args = {PresetDataContract.PresetEntry.PRESET_NAME};
        Cursor crs = db.query(true, PresetDataContract.PresetEntry.TABLE_NAME, args, null, null, null, null, null, null);
        crs.moveToFirst();
        int col = crs.getColumnIndex(PresetDataContract.PresetEntry.PRESET_NAME);
        for(int i =0; i < crs.getCount(); i++)
        {
            if(col != -1) {
                String str = crs.getString(col);
                if(str != null) {
                    list.add(str);
                }
            }
            crs.moveToNext();
        }
        crs.close();
        return list;
    }


    public float [] getPresetValues( String presetName)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        String[] args={presetName};
        Cursor crs = db.rawQuery("SELECT * FROM "   + PresetDataContract.PresetEntry.TABLE_NAME  + " WHERE "
                                                    + PresetDataContract.PresetEntry.PRESET_NAME  + " = ?", args);
        crs.moveToFirst();
        float [] v = new float[52];
        for(int i = 0; i < 52; i++)
        {
            v[i] = crs.getFloat(crs.getColumnIndex(PresetDataContract.PresetEntry.PRESET_VALUE));
            crs.moveToNext();
        }
        crs.close();
        return v;
    }

}