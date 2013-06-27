package com.livejournal.karino2.whiteboardcast;

import com.google.libwebm.mkvmuxer.MkvMuxer;
import com.livejournal.karino2.whiteboardcast.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Timer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class WhiteBoardCastActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private Timer timer = null;
    private EncoderTask encoderTask = null;

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG ).show();

    }

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(new Runnable(){

            @Override
            public void run() {
                int[] major = new int[2];
                int[] minor = new int[2];
                int[] build = new int[2];
                int[] revision = new int[2];
                // MkvMuxer.makeUid(20);
                MkvMuxer.getVersion(major, minor, build, revision);
                final String outStr = "libwebm:" + Integer.toString(major[0]) + "." +
                        Integer.toString(minor[0]) + "." + Integer.toString(build[0]) + "." +
                        Integer.toString(revision[0]);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMessage(outStr);
                    }
                }, 5000);

            }
        }).start();

        setContentView(R.layout.activity_whiteboardcast);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.record_start_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.record_stop_button).setOnTouchListener(mDelayHideTouchListener);

        findButton(R.id.record_start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = new Timer();
                WhiteBoardCanvas wb = (WhiteBoardCanvas)findViewById(R.id.fullscreen_content);
                encoderTask = new EncoderTask(wb, wb.getBitmap());
                if(!encoderTask.initEncoder()) {
                    showMessage("init encode fail");
                    return;
                }

                timer.scheduleAtFixedRate(encoderTask, 0, 1000/FPS);
                showMessage("record start");
            }
        });
        findButton(R.id.record_stop_button).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showMessage("record end");
                timer.cancel();

                handler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        /*
                        if(!encoderTask.doneEncoder()) {
                            showMessage("done encoder fail");
                        }
                        // for debug.
                        if(encoderTask.getErrorBuf().length() != 0) {
                            showMessage("error: " + encoderTask.getErrorBuf().toString());
                        }
                        */

                    }
                }, 5000);

            }
        });
        findButton(R.id.record_done_button).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(!encoderTask.doneEncoder()) {
                    showMessage("done encoder fail");
                }
                // for debug.
                if(encoderTask.getErrorBuf().length() != 0) {
                    showMessage("error: " + encoderTask.getErrorBuf().toString());
                }

            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            mSystemUiHider.toggle();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private final int FPS = 3;

    private Button findButton(int id) {
        return (Button)findViewById(id);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
