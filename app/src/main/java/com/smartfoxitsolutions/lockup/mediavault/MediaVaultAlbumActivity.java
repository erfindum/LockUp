package com.smartfoxitsolutions.lockup.mediavault;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.smartfoxitsolutions.lockup.LockUpMainActivity;
import com.smartfoxitsolutions.lockup.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class MediaVaultAlbumActivity extends AppCompatActivity {

    private static final int REQUEST_READ_WRITE_EXTERNAL_PERMISSION= 3;
    public static final int TYPE_IMAGE_MEDIA = 5;
    public static final int TYPE_AUDIO_MEDIA = 8;
    public static final int TYPE_VIDEO_MEDIA = 14;

    private Toolbar toolBar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton mediaVaultFab;
    private String[] tabTitle;
    private ExecutorService vaultExecutor;
    private SetVaultTask vaultTask;
    private String mediaType;
    boolean shouldTrackUserPresence, shouldCloseAffinity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vault_album_activity);
        shouldTrackUserPresence = true;
        toolBar = (Toolbar) findViewById(R.id.media_vault_activity_tool_bar);
        tabLayout = (TabLayout) findViewById(R.id.media_vault_activity_tab_layout);
        viewPager = (ViewPager) findViewById(R.id.media_vault_activity_viewPager);
        mediaVaultFab = (FloatingActionButton) findViewById(R.id.media_vault_activity_fab);
        mediaType = getIntent().getStringExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY);
        setSupportActionBar(toolBar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.vault_album_picker_select_album_toolbar);
        tabTitle = getResources().getStringArray(R.array.media_vault_tab_text);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
        setPermission();
        }else{
            if(mediaType!=null){
            displayVaultScreens(mediaType);
            }else{
            displayVaultScreens(MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA);
            }
        }
            setupVaultFolder();

    }

    void setFabListener(){
        mediaVaultFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMediaFetcherActivity();
            }
        });
        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    void startMediaFetcherActivity(){
        switch (viewPager.getCurrentItem()){
            case 0:
                startActivity(new Intent(this,MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA));
                shouldTrackUserPresence = false;
                break;
            case 1:
                startActivity(new Intent(this,MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA));
                shouldTrackUserPresence = false;
                break;
            case 2:
                startActivity(new Intent(this,MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA));
                shouldTrackUserPresence = false;
                break;
        }
    }

    @TargetApi(23)
    void setPermission(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,"android.permission.WRITE_EXTERNAL_STORAGE")
                    != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale
                        (this,"android.permission.WRITE_EXTERNAL_STORAGE")){
                    DialogFragment storagePermissionDialog = new ExternalStoragePermissionDialog();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
                    storagePermissionDialog.show(fragmentTransaction,"storage_read_write_dialog");
                }
                else{
                    requestReadPermission();
                }

            }else{
                if(mediaType!=null){
                    displayVaultScreens(mediaType);
                }else{
                    displayVaultScreens(MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA);
                }
            }
        }
    }

    void requestReadPermission(){
        ActivityCompat.requestPermissions(this,new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}
                                        ,REQUEST_READ_WRITE_EXTERNAL_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_READ_WRITE_EXTERNAL_PERMISSION){
            if(grantResults.length>=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                displayVaultScreens(MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA);
            }
            else{
                permissionDenied();
            }
        }
    }

    void displayVaultScreens(String mediaType){
        viewPager.setAdapter(new MediaVaultAlbumPagerAdapter(getSupportFragmentManager(),tabTitle));
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(getCurrentVaultScreen(mediaType));
        tabLayout.setupWithViewPager(viewPager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.media_vault_toolbar_title);
        setFabListener();
    }

    int getCurrentVaultScreen(String mediaType){
        switch (mediaType){
            case MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA:
                return 1;
            case MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA:
                return 2;
            case MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA:
                return 0;
        }
        return 1;
    }

    void setupVaultFolder(){
        vaultExecutor = Executors.newSingleThreadExecutor();
        vaultTask = new SetVaultTask();
        vaultExecutor.submit(vaultTask);
    }

     class SetVaultTask implements Runnable{

        @Override
        public void run() {
            String[] folders = {".image",".audio",".video"};
            File lockDirectory = new File(Environment.getExternalStorageDirectory()+File.separator
                                    +".lockup");
            boolean isDirectoryCreated = false;
            if(!lockDirectory.exists()){
               isDirectoryCreated = lockDirectory.mkdirs();
            }
            boolean isNoMediaCreated = false;
            if(isDirectoryCreated){
                File nomedia = new File(lockDirectory.getPath()+File.separator+".nomedia");
                if (!nomedia.exists()){
                    try {
                        isNoMediaCreated = nomedia.createNewFile();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            if(isNoMediaCreated){
                for(String folderName:folders){
                    File folder = new File(lockDirectory.getPath()+File.separator+folderName);
                    if(!folder.exists()){
                     boolean created = folder.mkdir();
                        if(created){
                            File thumbDirectory = new File(folder.getPath()+File.separator+".thumbs");
                            thumbDirectory.mkdir();
                        }
                    }
                }
            }

        }
    }

    void permissionDenied(){
        finish();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        shouldTrackUserPresence = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(shouldTrackUserPresence){
            shouldCloseAffinity = true;
        }else{
            shouldCloseAffinity = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(vaultExecutor!=null && !vaultExecutor.isShutdown()){
            vaultExecutor.shutdown();
        }
        if(vaultTask!=null){
            vaultTask = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(shouldCloseAffinity){
            finishAffinity();
        }
    }

    @Override
    public void onBackPressed() {
        shouldCloseAffinity = true;
        startActivity(new Intent(getBaseContext(), LockUpMainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        super.onBackPressed();
    }
}
