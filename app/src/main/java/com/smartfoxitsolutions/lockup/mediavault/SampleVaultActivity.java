package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 26-10-2016.
 */

public class SampleVaultActivity extends AppCompatActivity {
    Button imageButton, videoButton, audioButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_vault_activity);
        imageButton = (Button) findViewById(R.id.media_button1);
        videoButton = (Button) findViewById(R.id.media_button2);
        audioButton = (Button) findViewById(R.id.media_button3);
        setListener();
    }

    void setListener(){
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),MediaAlbumPickerActivity.class)
                                .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,
                                            MediaAlbumPickerActivity.TYPE_IMAGE_MEDIA));
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,
                                MediaAlbumPickerActivity.TYPE_VIDEO_MEDIA));
            }
        });

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(),MediaAlbumPickerActivity.class)
                        .putExtra(MediaAlbumPickerActivity.MEDIA_TYPE_KEY,
                                MediaAlbumPickerActivity.TYPE_AUDIO_MEDIA));
            }
        });
    }
}
