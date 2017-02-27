package com.smartfoxitsolutions.lockup;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.lang.ref.WeakReference;

/**
 * Created by RAAJA on 23-02-2017.
 */

public class WatchVideoActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    private YouTubePlayerFragment fragment;
    YouTubePlayer youtubePlayer;
    private boolean shouldTrackUserPresence, shouldCloseAffinity;
    private WatchVideoScreenOffReceiver watchVideoScreenOffReceiver;
    private TextView titleText;
    private AppCompatImageButton refreshButton,backButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watch_video_activity);
        shouldTrackUserPresence = true;
        titleText = (TextView) findViewById(R.id.watch_video_title);
        refreshButton = (AppCompatImageButton) findViewById(R.id.watch_video_refresh_button);
        backButton = (AppCompatImageButton) findViewById(R.id.watch_video_back_button);
        titleText.setText(getString(R.string.main_screen_activity_video_button_text));
    }

    @Override
    protected void onStart() {
        super.onStart();
        fragment = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.watch_video_fragment);
        initializeYoutubePlayer();
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeYoutubePlayer();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        watchVideoScreenOffReceiver = new WatchVideoActivity.WatchVideoScreenOffReceiver(new WeakReference<>(this));
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(watchVideoScreenOffReceiver,filter);
    }

    private void initializeYoutubePlayer(){
        fragment.initialize(getString(R.string.lockup_api_key),this);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (shouldTrackUserPresence) {
            shouldCloseAffinity = true;
        } else {
            shouldCloseAffinity = false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(getFragmentManager().getBackStackEntryCount()==0){
            finish();
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean b) {
        youtubePlayer = youTubePlayer;
        youtubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION);
        youTubePlayer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(String s) {
                youTubePlayer.play();
            }

            @Override
            public void onAdStarted() {
            }

            @Override
            public void onVideoStarted() {

            }

            @Override
            public void onVideoEnded() {

            }

            @Override
            public void onError(YouTubePlayer.ErrorReason errorReason) {

            }
        });
        if(!b){
            youTubePlayer.cueVideo("EbNGrF-dIgk");
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        if(youTubeInitializationResult.isUserRecoverableError()){
            youTubeInitializationResult.getErrorDialog(this,37).show();
        }else {
            Toast.makeText(this,youTubeInitializationResult.toString()+" "+getString(R.string.watch_video_playback_error_info)
                    ,Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 37){
            initializeYoutubePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(shouldCloseAffinity){
            finishAffinity();
        }
        Log.d("youtube","Youtube Player Closing -------------------");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(25,1,Menu.NONE,"Refresh");
        item.setIcon(R.drawable.ic_refresh_white);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if(item.getGroupId()==25){
            fragment.initialize(getString(R.string.lockup_api_key),this);
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(watchVideoScreenOffReceiver);
    }

    static class WatchVideoScreenOffReceiver extends BroadcastReceiver {

        WeakReference<WatchVideoActivity> activity;
        WatchVideoScreenOffReceiver(WeakReference<WatchVideoActivity> activity){
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                activity.get().finishAffinity();
            }
        }
    }
}
