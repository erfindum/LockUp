package com.smartfoxitsolutions.lockup.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;
import com.smartfoxitsolutions.lockup.AppLockModel;
import com.smartfoxitsolutions.lockup.DimensionConverter;
import com.smartfoxitsolutions.lockup.LockUpSettingsActivity;
import com.smartfoxitsolutions.lockup.R;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by RAAJA on 27-10-2016.
 */

public class LockPatternViewFinger extends FrameLayout implements PatternLockView.OnPatternChangedListener {
    private OnPinLockUnlockListener patternLockListener;
    private OnFingerScannerCancelListener fingerCanceledListener;

    private PatternLockView patternView;
    private ImageView appIconView;
    private String selectedPatternNode, patternPassCode, salt;
    private int patternNodeSelectedCount;
    boolean isVibratorEnabled, shouldHidePatternLine;
    private Vibrator patternViewVibrator;
    private FingerprintManagerCompat fingerprintManager;
    private CancellationSignal cancelSignal;
    private ValueAnimator patternAnimator;
    private RelativeLayout patternViewParent;
    private RelativeLayout patternAdParent;
    private AppCompatImageView fingerPrintIcon;
    private TextView fingerPrintInfoText;
    private AppCompatImageButton fingerPrintSwitchButton, pinPatternSwitchButton;
    private boolean isFingerPrintActive, shouldShowBigIcon, shouldShowAd;
    private int noOfAttempts, noOfNoisyAttempts;
    private ValueAnimator animatorMain, animatorFingerError;
    private RectF patternRect;
    private View adView;

    public LockPatternViewFinger(Context context, OnPinLockUnlockListener patternLockListener
            , OnFingerScannerCancelListener fingerCanceledListener, boolean isFingerPrintActive) {
        super(context);
        setPinLockUnlockListener(patternLockListener);
        setFingerCanceledListener(fingerCanceledListener);
        this.isFingerPrintActive = isFingerPrintActive;
        LayoutInflater.from(context).inflate(R.layout.pattern_lock_activity_finger, this, true);
        patternViewParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_finger_parent_view);
        patternAdParent = (RelativeLayout) findViewById(R.id.pattern_lock_activity_finger_ad_parent);
        patternViewVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        fingerprintManager = FingerprintManagerCompat.from(context);
        cancelSignal = new CancellationSignal();
        initializeLockView();
    }


    void setPinLockUnlockListener(OnPinLockUnlockListener pinLockListener) {
        this.patternLockListener = pinLockListener;
    }

    void setFingerCanceledListener(OnFingerScannerCancelListener fingerListener) {
        this.fingerCanceledListener = fingerListener;
    }

    public void setPackageName(String packageName) {
        patternLockListener.onPinLocked(packageName);
        setAppIcon(packageName);
    }

    public void setWindowBackground(Integer colorVibrant, int displayHeight) {
        if (colorVibrant == null) {
            colorVibrant = Color.parseColor("#2874F0");
        }
        GradientDrawable drawable = new GradientDrawable();
        int[] colors = {colorVibrant, Color.parseColor("#263238")};
        drawable.setColors(colors);
        float radius = Math.round(displayHeight * .95);
        drawable.setGradientRadius(radius);
        drawable.setGradientCenter(0.5f, 1f);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setShape(GradientDrawable.RECTANGLE);
        patternViewParent.setBackground(drawable);
    }

    void initializeLockView() {
        patternView = (PatternLockView) findViewById(R.id.pattern_lock_activity_finger_pattern_view);
        fingerPrintIcon = (AppCompatImageView) findViewById(R.id.pattern_lock_activity_finger_image_main);
        fingerPrintInfoText = (TextView) findViewById(R.id.pattern_lock_activity_finger_info);
        fingerPrintSwitchButton = (AppCompatImageButton) findViewById(R.id.pattern_lock_activity_finger_switch_finger);
        pinPatternSwitchButton = (AppCompatImageButton) findViewById(R.id.pattern_lock_activity_finger_switch_keyboard);
        appIconView = (ImageView) findViewById(R.id.pattern_lock_activity_finger_app_icon_view);
        selectedPatternNode = "";
        SharedPreferences prefs = getContext().getSharedPreferences(AppLockModel.APP_LOCK_PREFERENCE_NAME, Context.MODE_PRIVATE);
        isVibratorEnabled = prefs.getBoolean(LockUpSettingsActivity.VIBRATOR_ENABLED_PREFERENCE_KEY, false);
        shouldHidePatternLine = prefs.getBoolean(LockUpSettingsActivity.HIDE_PATTERN_LINE_PREFERENCE_KEY, false);
        if (shouldHidePatternLine) {
            patternView.setLinePaintTransparency(0);
        }
        patternPassCode = prefs.getString(AppLockModel.USER_SET_LOCK_PASS_CODE, "noPin");
        salt = prefs.getString(AppLockModel.DEFAULT_APP_BACKGROUND_COLOR_KEY, "noColor");

        registerListeners();
        setPatternAnimator();
        if (isFingerPrintActive) {
            pinPatternSwitchButton.setVisibility(VISIBLE);
            fingerPrintSwitchButton.setVisibility(INVISIBLE);
            switchToFingerView();
        } else {
            fingerPrintSwitchButton.setVisibility(INVISIBLE);
            pinPatternSwitchButton.setVisibility(INVISIBLE);
            switchToPatternView();
        }

    }

    void setAppIcon(String packageName) {
        try {
            // Log.d(AppLockingService.TAG,"App Icon CAlled " + packageName);
            Drawable appIcon = getContext().getPackageManager().getApplicationIcon(packageName);
            appIconView.setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    void registerListeners() {
        patternView.setOnPatternChangedListener(this);
        fingerPrintSwitchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToFingerView();
                fingerPrintSwitchButton.setVisibility(INVISIBLE);
                pinPatternSwitchButton.setVisibility(VISIBLE);
            }
        });

        pinPatternSwitchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToPatternView();
                fingerPrintSwitchButton.setVisibility(VISIBLE);
                pinPatternSwitchButton.setVisibility(INVISIBLE);
            }
        });
        patternRect = new RectF();
        patternViewParent.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (patternRect.contains(event.getX(), event.getY())) {
                    event.setLocation(event.getX() - patternRect.left, event.getY() - patternRect.top);
                    patternView.onTouchEvent(event);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    event.setLocation(event.getX() - patternRect.left, event.getY() - patternRect.top);
                    patternView.onTouchEvent(event);
                    return true;
                }
                return true;
            }
        });
        final ViewTreeObserver viewTreeObserver = patternViewParent.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    patternRect.set(patternView.getLeft(), patternView.getTop()
                            , patternView.getRight(), patternView.getBottom());
                    if (shouldShowBigIcon) {
                        addBigAppIcon();
                    }
                    if(shouldShowAd){
                        addAdView();
                    }
                    patternViewParent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    void unRegisterListeners() {
        patternView.setOnPatternChangedListener(null);
        pinPatternSwitchButton.setOnClickListener(null);
        fingerPrintSwitchButton.setOnClickListener(null);
        patternViewParent.setOnTouchListener(null);
        patternAnimator.removeAllUpdateListeners();
        appIconView.setImageDrawable(null);
    }

    private void switchToPatternView() {
        fingerPrintIcon.setVisibility(INVISIBLE);
        fingerPrintInfoText.setVisibility(INVISIBLE);
        patternView.setVisibility(VISIBLE);
        patternView.setEnabled(true);
        fingerPrintIcon.setEnabled(false);
        fingerPrintInfoText.setEnabled(false);
        cancelSignal.cancel();
    }

    private void switchToFingerView() {
        patternView.setVisibility(INVISIBLE);
        fingerPrintIcon.setVisibility(VISIBLE);
        fingerPrintInfoText.setVisibility(VISIBLE);
        fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
        fingerPrintIcon.setEnabled(true);
        fingerPrintInfoText.setEnabled(true);
        patternView.setEnabled(false);
        if (cancelSignal.isCanceled()) {
            fingerCanceledListener.onFingerScannerCanceled();
        }
    }

    void setPatternAnimator() {
        patternAnimator = ValueAnimator.ofFloat(0, 1);
        patternAnimator.setDuration(200).setRepeatCount(3);
        patternAnimator.setInterpolator(new OvershootInterpolator());
        patternAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                patternView.postPatternError();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                patternView.resetPatternView();
                resetPatternData();
            }
        });
    }

    public void addNativeAd(NativeAd nativeAd, final OnLockAdImpressedClicked onLockAdImpressedClicked) {
        if (nativeAd == null) {
            shouldShowBigIcon = true;
            return;
        }
        adView = nativeAd.createAdView(getContext(), null);
        nativeAd.renderAdView(adView);
        nativeAd.prepare(adView);
        nativeAd.setMoPubNativeEventListener(new NativeAd.MoPubNativeEventListener() {
            @Override
            public void onImpression(View view) {
                if(onLockAdImpressedClicked==null){
                    return;
                }

                onLockAdImpressedClicked.onAdImpressed();
            }

            @Override
            public void onClick(View view) {
                if(onLockAdImpressedClicked==null){
                    return;
                }

                onLockAdImpressedClicked.onAdClicked();
            }
        });

        shouldShowAd = true;
    }

    private void addAdView(){
        patternAdParent.addView(adView);
        Log.d("AppLock","Parent Height "+ getMeasuredHeight()+" " +getHeight());
    }


    private void addBigAppIcon() {
        int size = Math.round(DimensionConverter.convertDpToPixel(60, getContext()));
        int margin = ((patternViewParent.getHeight() - patternView.getHeight()) / 2) - (size / 2);
        RelativeLayout.LayoutParams parms = (RelativeLayout.LayoutParams) appIconView.getLayoutParams();
        parms.height = size;
        parms.width = size;
        parms.bottomMargin = margin;
        appIconView.setLayoutParams(parms);
    }

    @Override
    public void onPatternNodeSelected(int selectedPatternNode) {
        this.selectedPatternNode = this.selectedPatternNode + String.valueOf(selectedPatternNode);
        // Log.d("AppLock","Selected Pattern "+ this.selectedPatternNode);
        patternNodeSelectedCount = patternNodeSelectedCount + 1;
        if (isVibratorEnabled) {
            patternViewVibrator.vibrate(30);
        }
    }

    @Override
    public void onPatternCompleted(boolean patternCompleted) {
        if (patternCompleted && !selectedPatternNode.equals("")) {
            // Log.d("PatternLock Confirm",selectedPatternNode);
            if (patternPassCode.equals(validatePassword(selectedPatternNode))) {
                //    Log.d("AppLock",selectedPatternNode + " " + patternPassCode);
                patternView.resetPatternView();
                resetPatternData();
                postPatternCompleted();
            } else {
                patternAnimator.start();
            }
        }
    }

    private String validatePassword(String userPass) {
        StringBuilder builder = new StringBuilder();
        try {
            byte[] usePassByte = (userPass + salt).getBytes("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = digest.digest(usePassByte);
            for (int i = 0; i < messageDigest.length; ++i) {
                builder.append(Integer.toHexString((messageDigest[i] & 0xFF) | 0x100).substring(1, 3));
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    @Override
    public void onErrorStop() {
        if (patternAnimator.isStarted()) {
            patternAnimator.end();
            patternAnimator.cancel();
            patternView.resetPatternView();
            resetPatternData();
        }
    }

    void resetPatternData() {
        selectedPatternNode = "";
        patternNodeSelectedCount = 0;
    }

    @TargetApi(23)
    void analyzeFingerPrint() {
        final Resources res = getResources();
        if ((getContext().checkSelfPermission("android.permission.USE_FINGERPRINT") == PackageManager.PERMISSION_GRANTED)) {
            if (fingerprintManager.isHardwareDetected()) {
                if (fingerprintManager.hasEnrolledFingerprints()) {
                    fingerprintManager.authenticate(null, 0, cancelSignal, new FingerprintManagerCompat.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errMsgId, CharSequence errString) {
                            super.onAuthenticationError(errMsgId, errString);
                            if (!cancelSignal.isCanceled()) {
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                                    errorSwitchPatternView();
                                    animateFingerPrintToPattern(res.getString(R.string.finger_print_lock_main_failed_locked_pattern));

                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE) {
                                    errorSwitchPatternView();
                                    animateFingerPrintToPattern(res.getString(R.string.finger_print_lock_main_error_hardware_unavailable_pattern));

                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS) {
                                    fingerPrintInfoText.setText(res.getString(R.string.finger_print_lock_main_error_unable_process));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT) {
                                    fingerPrintInfoText.setText(res.getString(R.string.finger_print_lock_main_error_timeout));
                                }
                                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                                    errorSwitchPatternView();
                                    animateFingerPrintToPattern(res.getString(R.string.finger_print_lock_main_error_cancelled_pattern));

                                }
                            }
                        }

                        @Override
                        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                            super.onAuthenticationHelp(helpMsgId, helpString);
                            if (helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_INSUFFICIENT) {
                                if (noOfNoisyAttempts < 2) {
                                    animateMainInfo(res.getString(R.string.finger_print_lock_main_help_noisy_general));
                                    ++noOfNoisyAttempts;
                                }
                                if (noOfNoisyAttempts >= 2) {
                                    animateMainInfo(res.getString(R.string.finger_print_lock_main_help_dirty));
                                    noOfNoisyAttempts = 0;
                                }
                            }
                            if (helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_IMAGER_DIRTY) {
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_help_dirty));
                            }
                            if (helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_PARTIAL) {
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_help_partial));
                            }
                            if (helpMsgId == FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST) {
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_help_fast));
                            }

                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            postPatternCompleted();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            if (noOfAttempts < 2) {
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_failed_general));
                                ++noOfAttempts;
                            }
                            if (noOfAttempts >= 2) {
                                animateMainInfo(res.getString(R.string.finger_print_lock_main_failed_attempt_two));
                            }
                        }
                    }, null);
                } else {
                    errorSwitchPatternView();
                }

            } else {
                errorSwitchPatternView();
            }
        }
    }

    void errorSwitchPatternView() {
        fingerPrintSwitchButton.setVisibility(INVISIBLE);
        pinPatternSwitchButton.setVisibility(INVISIBLE);
        switchToPatternView();
    }

    void animateFingerPrintToPattern(final String infoText) {
        animatorFingerError = ValueAnimator.ofFloat(0, 1f);
        animatorFingerError.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fingerPrintInfoText.setVisibility(INVISIBLE);
                patternView.setVisibility(VISIBLE);
                fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                patternView.setVisibility(INVISIBLE);
                fingerPrintInfoText.setVisibility(VISIBLE);
                fingerPrintInfoText.setText(infoText);
                if (animatorMain != null && animatorMain.isRunning()) {
                    animatorMain.removeAllListeners();
                    animatorMain.end();
                    animatorMain.cancel();
                }
            }
        });
        animatorFingerError.setInterpolator(new LinearInterpolator());
        animatorFingerError.setDuration(2500);
        animatorFingerError.start();
    }

    void animateMainInfo(final String infoText) {
        animatorMain = ValueAnimator.ofFloat(0, 1f);
        animatorMain.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                fingerPrintInfoText.setText(getResources().getString(R.string.finger_print_lock_main_info));
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                fingerPrintInfoText.setText(infoText);
            }
        });
        animatorMain.setInterpolator(new LinearInterpolator());
        animatorMain.setDuration(2500);
        animatorMain.start();
    }

    public void updateCancelSignal() {
        if (isFingerPrintActive) {
            cancelSignal = null;
            cancelSignal = new CancellationSignal();
            analyzeFingerPrint();
        }
    }

    private void postPatternCompleted() {
        if (patternLockListener != null) {
            patternLockListener.onPinUnlocked();
        }
    }

    void startHome() {
        getContext().startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        //Log.d("LockUp","Called Start Home");

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event);
        }
        startHome();
        return true;
    }

    public void removeView() {
        unRegisterListeners();
        patternView.closePatternView();
        if (adView != null) {
            patternAdParent.removeView(adView);
        }
        setPinLockUnlockListener(null);
        setFingerCanceledListener(null);
        if (cancelSignal != null && !cancelSignal.isCanceled()) {
            cancelSignal.cancel();
        }
    }

}
