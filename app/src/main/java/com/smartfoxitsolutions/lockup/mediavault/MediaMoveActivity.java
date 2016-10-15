package com.smartfoxitsolutions.lockup.mediavault;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveActivity extends AppCompatActivity {

    public static final String VAULT_TYPE_KEY = "vault_move_type";
    public static final int MOVE_TYPE_INTO_VAULT = 5;
    public static final int MOVE_TYPE_OUT_OF_VAULT = 6;

    public static final String SERVICE_START_TYPE_KEY = "service_start_type";
    public static final int SERVICE_START_TYPE_NEW = 10;

    public static final String MEDIA_FILE_NAMES_KEY = "media_file_names";

    public static final String MEDIA_SELECTION_TYPE = "media_selection_type";
    public static final int MEDIA_SELECTION_TYPE_ALL = 8;
    public static final int MEDIA_SELECTION_TYPE_UNIQUE = 9;

    public static final String MEDIA_MOVE_MESSENGER_KEY = "media_move_messenger";

    public static final String MEDIA_MOVE_ACTION  = "media_move_action";


    TextView countText;
    Button doneButton;
    TextView moveText;
    int moveType, mediaSelectionType, serviceStartType, selectedFileCount;
    String albumBucketId, mediaType;
    String[] selectedMediaId,fileNames;
    AtomicLong timestamp;
    MoveReceiver receiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_move_activity);
        countText = (TextView) findViewById(R.id.media_move_activity_move_count);
        doneButton = (Button) findViewById(R.id.media_move_activity_button_done);
        moveText = (TextView) findViewById(R.id.media_move_activity_move_text);
        timestamp = new AtomicLong(System.currentTimeMillis());
        Intent intent = getIntent();
        serviceStartType = intent.getIntExtra(SERVICE_START_TYPE_KEY,0);
        disableDoneButton();
        setMoveText();
        receiver = new MoveReceiver(getWeakReference());
        if(serviceStartType==SERVICE_START_TYPE_NEW){
            moveType = intent.getIntExtra(VAULT_TYPE_KEY,2);
            mediaSelectionType = intent.getIntExtra(MEDIA_SELECTION_TYPE,2);
            selectedFileCount = intent.getIntExtra(MediaAlbumPickerActivity.SELECTED_FILE_COUNT_KEY,0);
            albumBucketId = intent.getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
            mediaType = intent.getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
            if(mediaSelectionType==MEDIA_SELECTION_TYPE_UNIQUE) {
                selectedMediaId = intent.getStringArrayExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY);
            }
            boolean isFileNameSet = setFileNames();
            if(isFileNameSet){
               startMoveService();
            }
        }
        registerListeners();
    }

    void setMoveText(){
        if(moveType==MOVE_TYPE_INTO_VAULT){
            moveText.setText(R.string.vault_move_activity_move_in_text);
        }
        if(moveType == MOVE_TYPE_OUT_OF_VAULT){
            moveText.setText(R.string.vault_move_activity_move_out_text);
        }
    }

    void disableDoneButton(){
        doneButton.setEnabled(false);
        doneButton.setTextColor(Color.parseColor("#1565C0"));
    }

    void enableDoneButton(){
        doneButton.setEnabled(true);
        doneButton.setTextColor(Color.parseColor("#2874F0"));
    }

    void registerListeners(){
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVaultHome();
            }
        });
    }

    private long getFileTimeStamp(){
        long currentTime = System.currentTimeMillis();
        if(timestamp.get()>=currentTime){
            currentTime = timestamp.get()+1;
        }
        if(timestamp.compareAndSet(timestamp.get(),currentTime)){
            return currentTime;
        }
        return 0;
    }

    private boolean setFileNames(){
        fileNames = new String[selectedFileCount];
        for(int i =0;i<fileNames.length;i++){
            fileNames[i] = String.valueOf(getFileTimeStamp());
        }
        return true;
    }

    void startVaultHome(){
        startActivity(new Intent(this,MediaVaultActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    void startMoveService(){
        Intent serviceIntent;
        if(moveType==MOVE_TYPE_INTO_VAULT) {
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_ALL) {
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL)
                        .putExtra(SERVICE_START_TYPE_KEY, SERVICE_START_TYPE_NEW)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY, albumBucketId)
                        .putExtra(MEDIA_FILE_NAMES_KEY,fileNames)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, mediaType)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY, MediaMoveActivity.MOVE_TYPE_INTO_VAULT);
                startService(serviceIntent);
            }
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_UNIQUE){
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE)
                        .putExtra(SERVICE_START_TYPE_KEY, SERVICE_START_TYPE_NEW)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                        .putExtra(MEDIA_FILE_NAMES_KEY,fileNames)
                        .putExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY,selectedMediaId)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT);
                startService(serviceIntent);
            }
        }
        if(moveType == MOVE_TYPE_OUT_OF_VAULT){

        }
    }

    WeakReference<MediaMoveActivity> getWeakReference(){
        return new WeakReference<>(this);
    }


    static  class MoveReceiver extends BroadcastReceiver{
        WeakReference<MediaMoveActivity> activity;
        MoveReceiver(WeakReference<MediaMoveActivity> activity){
            this.activity = activity;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MEDIA_MOVE_ACTION)){
                int moveType = intent.getIntExtra(MediaMoveService.MEDIA_MOVE_SUCCESS_TYPE_KEY,0);
                if(moveType == MediaMoveService.MEDIA_SUCCESSFULLY_MOVED){
                    int arg1 = intent.getIntExtra(MediaMoveService.MEDIA_MOVE_COUNT_KEY,0);
                    int arg2 = intent.getIntExtra(MediaMoveService.MEDIA_MOVE_TOTAL_COUNT_KEY,0);
                    String moveText = "Files " + arg1 + " of " + arg2;
                    activity.get().countText.setText(moveText);
                }
                if(moveType == MediaMoveService.MEDIA_MOVE_COMPLETED){
                    activity.get().countText.setText(R.string.vault_move_activity_move_complete);
                    activity.get().enableDoneButton();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(MEDIA_MOVE_ACTION);
        registerReceiver(receiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiver = null;
    }
}
