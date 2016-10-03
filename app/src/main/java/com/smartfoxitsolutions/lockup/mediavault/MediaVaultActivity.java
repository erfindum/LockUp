package com.smartfoxitsolutions.lockup.mediavault;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.smartfoxitsolutions.lockup.GrantUsageAccessDialog;

/**
 * Created by RAAJA on 22-09-2016.
 */
public class MediaVaultActivity extends AppCompatActivity {

    private static final int REQUEST_READ_WRITE_EXTERNAL_PERMISSION= 3;
    boolean storagePermissionGranted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
        setPermission();
        }else{
            startActivity(new Intent(this,SampleVaultActivity.class));
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
                startActivity(new Intent(this,SampleVaultActivity.class));
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
                startActivity(new Intent(this,SampleVaultActivity.class));
            }
            else{
                permissionDenied();
            }
        }
    }

    void permissionDenied(){
        finish();
    }
}
