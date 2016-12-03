package com.smartfoxitsolutions.lockup.mediavault;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.ShareAlertDialog;
import com.smartfoxitsolutions.lockup.mediavault.services.MediaMoveService;
import com.smartfoxitsolutions.lockup.mediavault.services.ShareMoveService;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by RAAJA on 13-10-2016.
 */

public class MediaMoveActivity extends AppCompatActivity {

    public static final String VAULT_TYPE_KEY = "vault_move_type";
    public static final int MOVE_TYPE_INTO_VAULT = 5;
    public static final int MOVE_TYPE_OUT_OF_VAULT = 6;
    public static final int MOVE_TYPE_DELETE_FROM_VAULT = 7;

    public static final String MEDIA_FILE_NAMES_KEY = "media_file_names";

    public static final String MEDIA_SELECTION_TYPE = "media_selection_type";
    public static final int MEDIA_SELECTION_TYPE_ALL = 8;
    public static final int MEDIA_SELECTION_TYPE_UNIQUE = 9;

    public static final String SHARE_MEDIA_FILE_LIST_KEY = "share_file_list";

    public static final String MEDIA_MOVE_MESSENGER_KEY = "media_move_messenger";

    TextView countText;
    ProgressBar countProgress;
    Button doneButton;
    TextView moveText, moveInfoText;
    int moveType, mediaSelectionType, selectedFileCount;
    String albumBucketId, mediaType;
    String[] selectedMediaId,fileNames;
    AtomicLong timestamp;
    boolean isOperationComplete,hasMoveStarted,shouldDisplayShareAlert;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_move_activity);
        countText = (TextView) findViewById(R.id.media_move_activity_move_count);
        countProgress = (ProgressBar) findViewById(R.id.media_move_activity_move_count_progress);
        doneButton = (Button) findViewById(R.id.media_move_activity_button_done);
        moveText = (TextView) findViewById(R.id.media_move_activity_move_text);
        moveInfoText = (TextView) findViewById(R.id.media_move_activity_move_info);
        Intent intent = getIntent();
        if(intent.getAction()!=null && intent.getAction().equals(Intent.ACTION_SEND)){
            if(!MediaMoveService.SERVICE_STARTED) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    ArrayList<Uri> fileUri = new ArrayList<>();
                    fileUri.add(uri);
                    startShareService(fileUri);
                }
                return;
            }else{
                shouldDisplayShareAlert = true;
            }
        }
        if(intent.getAction()!=null && intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)){
            if(!MediaMoveService.SERVICE_STARTED) {
                ArrayList<Uri> uriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (uriList != null && !uriList.isEmpty()) {
                   startShareService(uriList);
                }
                return;
            }else{
                shouldDisplayShareAlert = true;
            }
        }
        timestamp = new AtomicLong(System.currentTimeMillis());
        setMoveBackgroundButton();
        registerListeners();
        moveType = intent.getIntExtra(VAULT_TYPE_KEY,0);
        mediaSelectionType = intent.getIntExtra(MEDIA_SELECTION_TYPE,0);
        albumBucketId = intent.getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
        mediaType = intent.getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
        if(mediaSelectionType==MEDIA_SELECTION_TYPE_UNIQUE) {
            selectedMediaId = intent.getStringArrayExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY);
        }
        if(moveType == MOVE_TYPE_INTO_VAULT) {
            selectedFileCount = intent.getIntExtra(MediaAlbumPickerActivity.SELECTED_FILE_COUNT_KEY,0);
            setFileNames();
        }
        setMoveText();
        startMoveService();
    }

    void setMoveText(){
        if(moveType==MOVE_TYPE_INTO_VAULT){
            moveText.setText(R.string.vault_move_activity_move_in_text);
        }
        if(moveType == MOVE_TYPE_OUT_OF_VAULT){
            moveText.setText(R.string.vault_move_activity_move_out_text);
        }
        if(moveType == MOVE_TYPE_DELETE_FROM_VAULT){
            moveText.setText(getResources().getString(R.string.vault_move_activity_delete_files));
        }
    }

    void setMoveBackgroundButton(){
        doneButton.setText(getResources().getString(R.string.vault_move_activity_move_background));
        isOperationComplete = false;
    }

    void setDoneButton(){
        doneButton.setText(getResources().getString(R.string.vault_move_activity_move_button));
    }

    void registerListeners(){
        countProgress.setVisibility(View.VISIBLE);
        countText.setVisibility(View.INVISIBLE);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isOperationComplete){
                    startVaultHome();
                }else{
                    moveToBackground();
                }

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

    private void displayShareAlertDialog(){
        DialogFragment shareAlertDialog = new ShareAlertDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("share_alert_dialog");
        shareAlertDialog.show(fragmentTransaction,"share_alert_dialog");
        shouldDisplayShareAlert = false;
    }

    private void startShareService(ArrayList<Uri> fileUriList){
        Messenger messenger = new Messenger(new MoveHandler(getWeakReference()));
        startService(new Intent(getBaseContext(), ShareMoveService.class)
                    .putParcelableArrayListExtra(SHARE_MEDIA_FILE_LIST_KEY,fileUriList)
                    .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger));
        moveText.setText(R.string.vault_move_activity_move_in_text);
        setMoveBackgroundButton();
        registerListeners();
    }

    private void startVaultHome(){
        if(mediaType != null) {
            startActivity(new Intent(this, MediaVaultAlbumActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, mediaType));
        }else{
            startActivity(new Intent(this, MediaVaultAlbumActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        NotificationManager notificationManager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(7549682);
    }

    private void moveToBackground(){
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
    }

    private void startMoveService(){
        Messenger messenger = new Messenger(new MoveHandler(getWeakReference()));
        Intent serviceIntent;
        if(moveType==MOVE_TYPE_INTO_VAULT) {
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_ALL) {
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY, albumBucketId)
                        .putExtra(MEDIA_FILE_NAMES_KEY,fileNames)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, mediaType)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY, MediaMoveActivity.MOVE_TYPE_INTO_VAULT)
                        .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
            }
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_UNIQUE){
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                        .putExtra(MEDIA_FILE_NAMES_KEY,fileNames)
                        .putExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY,selectedMediaId)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT)
                        .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
            }
        }
        if(moveType == MOVE_TYPE_OUT_OF_VAULT){
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_ALL) {
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY, albumBucketId)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, mediaType)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY, MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT)
                        .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
            }
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_UNIQUE){
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                        .putExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY,selectedMediaId)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT)
                        .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
            }
        }
        if(moveType == MOVE_TYPE_DELETE_FROM_VAULT){
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_ALL) {
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_ALL)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY, albumBucketId)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, mediaType)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY, MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT)
                        .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
            }
            if(mediaSelectionType == MEDIA_SELECTION_TYPE_UNIQUE){
                serviceIntent = new Intent(this, MediaMoveService.class);
                serviceIntent.putExtra(MediaMoveActivity.MEDIA_SELECTION_TYPE, MediaMoveActivity.MEDIA_SELECTION_TYPE_UNIQUE)
                        .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                        .putExtra(MediaAlbumPickerActivity.SELECTED_MEDIA_FILES_KEY,selectedMediaId)
                        .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT)
                        .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
            }
        }
    }

    WeakReference<MediaMoveActivity> getWeakReference(){
        return new WeakReference<>(this);
    }

    static class MoveHandler extends Handler{
        WeakReference<MediaMoveActivity> activity;
        MoveHandler(WeakReference<MediaMoveActivity> activity){
            this.activity = activity;
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(activity.get().countText!=null) {
                if (msg.what == MediaMoveService.MEDIA_SUCCESSFULLY_MOVED) {
                    int arg1 = msg.arg1;
                    int arg2 = msg.arg2;
                    String moveText = "Files " + arg1 + " of " + arg2;
                    activity.get().countText.setText(moveText);
                    if(!activity.get().hasMoveStarted){
                        activity.get().countProgress.setVisibility(View.INVISIBLE);
                        activity.get().countText.setVisibility(View.VISIBLE);
                        activity.get().hasMoveStarted = true;
                    }
                }
                if (msg.what == MediaMoveService.MEDIA_MOVE_COMPLETED) {
                    activity.get().moveInfoText.setText(R.string.vault_move_activity_move_complete);
                    activity.get().isOperationComplete = true;
                    activity.get().setDoneButton();
                    activity.get().mediaType = (String) msg.obj;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(MediaMoveService.SERVICE_STARTED){
            Messenger messenger = new Messenger(new MoveHandler(getWeakReference()));
            MediaMoveService.updateMoveMessenger(messenger);
        }
        if(ShareMoveService.SERVICE_STARTED){
            Messenger messenger = new Messenger(new MoveHandler(getWeakReference()));
            ShareMoveService.updateShareMessenger(messenger);
        }
        if(shouldDisplayShareAlert){
            displayShareAlertDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(MediaMoveService.SERVICE_STARTED){
            MediaMoveService.updateMoveMessenger(null);
        }
        if(ShareMoveService.SERVICE_STARTED){
            ShareMoveService.updateShareMessenger(null);
        }
    }

    @Override
    public void onBackPressed() {
        if(MediaMoveService.SERVICE_STARTED || ShareMoveService.SERVICE_STARTED) {
            moveToBackground();
            return;
        }
        startVaultHome();
    }
}
