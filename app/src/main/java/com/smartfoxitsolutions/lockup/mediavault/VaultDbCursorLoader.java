package com.smartfoxitsolutions.lockup.mediavault;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v4.content.AsyncTaskLoader;

import java.io.File;

/**
 * Created by RAAJA on 15-10-2016.
 */

public class VaultDbCursorLoader extends AsyncTaskLoader<Cursor> {
    private Context appContext;
    private LoadReceiver loadReceiver;
    private VaultDbHelper dbHelper;
    private Cursor dbCursor, oldDbCursor;
    private String[] projection,selectionArgs;
    private String selection, orderBy;
    private boolean receiverRegistered;


    public VaultDbCursorLoader(Context context,int version, String[] projection, String selection, String[] selectionArgs
                            ,String orderBy) {
        super(context);
        appContext = context;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.orderBy = orderBy;
        String databasePath = Environment.getExternalStorageDirectory()+ File.separator
                +".lockup"+File.separator+"vault_db";
        dbHelper = new VaultDbHelper(context.getApplicationContext(),databasePath,null,version);
        loadReceiver = new LoadReceiver();
    }

    @Override
    public Cursor loadInBackground() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(MediaVaultModel.TABLE_NAME,projection,selection,selectionArgs,null,null,orderBy);
        if(cursor!=null){
            cursor.getCount();
        }
        return cursor;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if(loadReceiver != null){
            IntentFilter filter = new IntentFilter("com.smartfoxitsolutions.lockup.mediavault.FORCE_LOAD_LOADER");
            appContext.getApplicationContext().registerReceiver(loadReceiver,filter);
            receiverRegistered = true;
        }
        if(dbCursor != null){
            deliverResult(dbCursor);
        }
        if(dbCursor == null || takeContentChanged()){
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        receiverUnregister();
        cancelLoad();
    }

    void receiverUnregister(){
        if(loadReceiver != null && receiverRegistered){
            appContext.getApplicationContext().unregisterReceiver(loadReceiver);
            receiverRegistered = false;
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        stopLoading();
        closeResources();
    }

    @Override
    public void onCanceled(Cursor data) {
        super.onCanceled(data);
        if(data!=null && !data.isClosed()){
            data.close();
        }
    }

    @Override
    public void deliverResult(Cursor data) {
        super.deliverResult(data);
        if(isReset()){
            data.close();
            return;
        }

        oldDbCursor = dbCursor;
        dbCursor = data;
        if(isStarted()){
            super.deliverResult(dbCursor);
        }

        if(oldDbCursor!=null && oldDbCursor!=data && !oldDbCursor.isClosed()){
            oldDbCursor.close();
        }
    }

    private class LoadReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            onContentChanged();
        }
    }

    private void closeResources(){
        loadReceiver = null;
        if(dbCursor!=null && !dbCursor.isClosed()){
            dbCursor.close();
        }
        if(dbHelper!=null){
            dbHelper.close();
        }
    }
}
