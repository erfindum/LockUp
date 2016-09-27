package com.smartfoxitsolutions.lockup.mediavault;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class ImagePickerBucketActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(2,null,this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                                , MediaStore.Images.Media.BUCKET_ID,MediaStore.Images.Media.DATA};
        String orderBy = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC";

        return new CursorLoader(getBaseContext(),
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,projection,null,null,orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        logData(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    void logData(Cursor cursor){
        Log.d("AppLock",String.valueOf(cursor == null));
        if(cursor!=null && cursor.getCount()>=1){
            cursor.moveToFirst();
            while(cursor.moveToNext()){
                int bucketNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int bucketIdIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                Log.d("AppLock",cursor.getString(bucketNameIndex)+" " + cursor.getString(bucketIdIndex)
                            + " " + cursor.getString(dataIndex));
            }
        }
    }
}
