package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by RAAJA on 08-10-2016.
 */

public class VaultDbHelper extends SQLiteOpenHelper {

    public VaultDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(MediaVaultModel.getCreateSQLString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(db.getVersion()<newVersion){
            db.execSQL(MediaVaultModel.getUpgradeSQLString());
        }
    }
}
