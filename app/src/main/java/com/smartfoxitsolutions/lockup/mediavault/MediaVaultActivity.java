package com.smartfoxitsolutions.lockup.mediavault;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import com.smartfoxitsolutions.lockup.GrantUsageAccessDialog;
import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class MediaVaultActivity extends AppCompatActivity {

    private static final int REQUEST_READ_WRITE_EXTERNAL_PERMISSION= 3;
    public static final int TYPE_IMAGE_MEDIA = 5;
    public static final int TYPE_AUDIO_MEDIA = 8;
    public static final int TYPE_VIDEO_MEDIA = 14;

    private Toolbar toolBar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    FloatingActionButton mediaVaultFab;
    private String[] tabTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_vault_activity);
        toolBar = (Toolbar) findViewById(R.id.media_vault_activity_tool_bar);
        tabLayout = (TabLayout) findViewById(R.id.media_vault_activity_tab_layout);
        viewPager = (ViewPager) findViewById(R.id.media_vault_activity_viewPager);
        mediaVaultFab = (FloatingActionButton) findViewById(R.id.media_vault_activity_fab);
        setSupportActionBar(toolBar);
        tabTitle = getResources().getStringArray(R.array.media_vault_tab_text);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
        setPermission();
        }else{
            displayVaultScreens();
        }
    }

    void setFabListener(){
        mediaVaultFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMediaFetcherActivity();
            }
        });
    }

    void startMediaFetcherActivity(){
        switch (viewPager.getCurrentItem()){
            case 0:
                startActivity(new Intent(this,MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA));
                break;
            case 1:
                startActivity(new Intent(this,MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA));
                break;
            case 2:
                startActivity(new Intent(this,MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA));
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
                displayVaultScreens();
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
                displayVaultScreens();
            }
            else{
                permissionDenied();
            }
        }
    }

    void displayVaultScreens(){
        viewPager.setAdapter(new MediaVaultPagerAdapter(getSupportFragmentManager(),tabTitle));
        viewPager.setCurrentItem(1);
        tabLayout.setupWithViewPager(viewPager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.media_vault_toolbar_title);
        setFabListener();
    }

    void permissionDenied(){
        finish();
    }
}
