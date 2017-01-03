package com.smartfoxitsolutions.lockup.mediavault;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.MainLockActivity;
import com.smartfoxitsolutions.lockup.R;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.ExternalStoragePermissionDialog;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.ShareAlertDialog;
import com.smartfoxitsolutions.lockup.mediavault.dialogs.ShareMovePermissionDialog;
import com.smartfoxitsolutions.lockup.mediavault.services.MediaMoveService;
import com.smartfoxitsolutions.lockup.mediavault.services.ShareMoveService;

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


    public static final String SHARE_MEDIA_FILE_LIST_KEY = "share_file_list";

    public static final String MEDIA_MOVE_MESSENGER_KEY = "media_move_messenger";

    private TextView countText;
    private ProgressBar countProgress;
    private Button doneButton;
    private TextView moveText, moveInfoText;
    int moveType;
    private String albumBucketId, mediaType;
    private AtomicLong timestamp;
    boolean isOperationComplete,hasMoveStarted,shouldDisplayShareAlert;
    private AppCompatImageView moveInfoImage;
    private Messenger messenger;
    private ArrayList<Uri> fileUriList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_move_activity);
        countText = (TextView) findViewById(R.id.media_move_activity_move_count);
        countProgress = (ProgressBar) findViewById(R.id.media_move_activity_move_count_progress);
        doneButton = (Button) findViewById(R.id.media_move_activity_button_done);
        moveText = (TextView) findViewById(R.id.media_move_activity_move_text);
        moveInfoText = (TextView) findViewById(R.id.media_move_activity_move_info);
        moveInfoImage = (AppCompatImageView) findViewById(R.id.media_move_activity_image);
        Intent intent = getIntent();
        if(intent.getAction()!=null && intent.getAction().equals(Intent.ACTION_SEND)){
            if(!MediaMoveService.SERVICE_STARTED) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    fileUriList = new ArrayList<>();
                    fileUriList.add(uri);
                    startShareFileMove(fileUriList);
                }
                return;
            }else{
                shouldDisplayShareAlert = true;
            }
        }
        if(intent.getAction()!=null && intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)){
            if(!MediaMoveService.SERVICE_STARTED) {
               fileUriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (fileUriList != null && !fileUriList.isEmpty()) {
                   startShareFileMove(fileUriList);
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
        albumBucketId = intent.getStringExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY);
        mediaType = intent.getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
        setFileNames();
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

    private void setFileNames(){
        SelectedMediaModel selectedMediaModel = SelectedMediaModel.getInstance();
        ArrayList<String> fileNamesList = new ArrayList<>(selectedMediaModel.getSelectedMediaIdList().size());
        for(int i =0; i<selectedMediaModel.getSelectedMediaIdList().size();i++){
            fileNamesList.add(String.valueOf(getFileTimeStamp()));
        }
        selectedMediaModel.setSelectedMediaFileNameList(fileNamesList);
    }

    private void displayShareAlertDialog(){
        DialogFragment shareAlertDialog = new ShareAlertDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack("share_alert_dialog");
        shareAlertDialog.show(fragmentTransaction,"share_alert_dialog");
        shouldDisplayShareAlert = false;
    }

    private void startShareFileMove(ArrayList<Uri> fileUriList) {
        messenger = new Messenger(new MoveHandler(getWeakReference()));
        moveText.setText(R.string.vault_move_activity_move_in_text);
        moveInfoImage.setImageResource(R.drawable.ic_vault_share_icon);
        setMoveBackgroundButton();
        registerListeners();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getStoragePermission(fileUriList, messenger);
        }else{
            startShareMoveService(fileUriList,messenger);
        }
    }

    @TargetApi(23)
    void getStoragePermission(ArrayList<Uri> fileUriList, Messenger messenger){
            if (ContextCompat.checkSelfPermission(this,"android.permission.WRITE_EXTERNAL_STORAGE")
                    != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale
                        (this,"android.permission.WRITE_EXTERNAL_STORAGE")){
                    DialogFragment storagePermissionDialog = new ShareMovePermissionDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
                    storagePermissionDialog.show(fragmentTransaction,"storage_read_write_dialog");
                }
                else{
                    requestReadPermission();
                }

            }else{
               startShareMoveService(fileUriList,messenger);
            }
    }

    public void requestReadPermission(){
        ActivityCompat.requestPermissions(this,new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}
                ,MediaVaultAlbumActivity.REQUEST_READ_WRITE_EXTERNAL_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MediaVaultAlbumActivity.REQUEST_READ_WRITE_EXTERNAL_PERMISSION){
            if(grantResults.length>=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                messenger = new Messenger(new MoveHandler(getWeakReference()));
                startShareMoveService(fileUriList,messenger);
            }
            else{
                permissionDenied();
            }
        }
    }

    private void startShareMoveService(ArrayList<Uri> fileUriList, Messenger messenger){
        startService(new Intent(getBaseContext(), ShareMoveService.class)
                .putParcelableArrayListExtra(SHARE_MEDIA_FILE_LIST_KEY,fileUriList)
                .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger));
    }

    public void permissionDenied(){
        finish();
    }

    private void startVaultHome(){
        if(mediaType != null) {
            startActivity(new Intent(this, MediaVaultAlbumActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY, mediaType));
        }else{
            startActivity(new Intent(this, MainLockActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        NotificationManager notificationManager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(7549682);
        finish();
    }

    private void moveToBackground(){
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
    }

    private void startMoveService(){
        Messenger messenger = new Messenger(new MoveHandler(getWeakReference()));
        Intent serviceIntent;
        if(moveType==MOVE_TYPE_INTO_VAULT) {
            serviceIntent = new Intent(this, MediaMoveService.class);
            serviceIntent
                    .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                    .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                    .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_INTO_VAULT)
                    .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);

        }
        if(moveType == MOVE_TYPE_OUT_OF_VAULT){
            serviceIntent = new Intent(this, MediaMoveService.class);
            serviceIntent
                    .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                    .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                    .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT)
                    .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
                startService(serviceIntent);
        }
        if(moveType == MOVE_TYPE_DELETE_FROM_VAULT){
            serviceIntent = new Intent(this, MediaMoveService.class);
            serviceIntent
                    .putExtra(MediaAlbumPickerActivity.ALBUM_BUCKET_ID_KEY,albumBucketId)
                    .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,mediaType)
                    .putExtra(MediaMoveActivity.VAULT_TYPE_KEY,MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT)
                    .putExtra(MEDIA_MOVE_MESSENGER_KEY,messenger);
            startService(serviceIntent);
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
        if(moveType ==  MediaMoveActivity.MOVE_TYPE_INTO_VAULT){
            moveInfoImage.setImageResource(R.drawable.ic_vault_share_icon);
        }

        if(moveType == MediaMoveActivity.MOVE_TYPE_OUT_OF_VAULT || moveType ==  MediaMoveActivity.MOVE_TYPE_DELETE_FROM_VAULT){
            moveInfoImage.setImageResource(R.drawable.ic_vault_share_out_icon);
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
