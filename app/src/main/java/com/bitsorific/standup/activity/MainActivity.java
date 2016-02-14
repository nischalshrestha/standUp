package com.bitsorific.standup.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bitsorific.standup.R;
import com.bitsorific.standup.service.CountDownService;


public class MainActivity extends AppCompatActivity {

    // Constants
    public static final int MINUTE = 60000;
    public static final int MILLIS = 1000;
    private static final String TAG = "MainActivity";

    // Timer
    private TextView timerView;
    private TextView timerUnitView;
    private int timePeriodStand;
    private int timePeriodSit;
    private long remainingMillis;

    // Status
    private TextView statusTextView;
    private ImageView statusView;
    private Button startBtn;
    public static int standColor;
    public static int sitColor;
    private int currentStatus = sitColor;

    // Animation
    private Handler handler = new Handler();
    private ProgressBar progressBar;
    private int progress = 0;
    // The ring shape
    private GradientDrawable bgShape;

    // Drawables to reference to
    private Drawable sit;
    private Drawable stand;

    private SharedPreferences prefs;

    // Flag to update progress UI when resuming activity
    private boolean justIn = false;
    private boolean transition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up Views
        timerView = (TextView) findViewById(R.id.timer);
        timerUnitView = (TextView) findViewById(R.id.timerUnit);
        statusView = (ImageView) findViewById(R.id.status);
        statusTextView = (TextView) findViewById(R.id.statusText);
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
        timerView.setTypeface(typeFace);

        // Set up vars for TextView colors and images
        sitColor = getResources().getColor(R.color.cyan);
        standColor = getResources().getColor(R.color.orange);
        sit = getResources().getDrawable(R.drawable.ic_seat_recline_normal);
        stand = getResources().getDrawable(R.drawable.ic_walk);

        // Set up progress bar and button to start/stop timers
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        RotateDrawable rd = (RotateDrawable) progressBar.getProgressDrawable();
        bgShape = (GradientDrawable) rd.getDrawable();

        startBtn = (Button) findViewById(R.id.start);

        // Grab timer and sound settings
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // If we're not already running the timers, set views to default values
        if(!isMyServiceRunning(CountDownService.class)) {
            setDefaultViews();
        }

//        Log.d(TAG, "onCreate() called");

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startBtn.getText().equals(getString(R.string.start_button))) {
                    // Sit off service to start timer(s)
                    statusTextView.setText(R.string.time_to_sit);
                    timerUnitView.setText(R.string.timer_unit);
                    startBtn.setText(R.string.stop_button);
                    startService(new Intent(getApplicationContext(), CountDownService.class));
                    Log.i(TAG, "Started service");
                } else if (startBtn.getText().equals(getString(R.string.stop_button))) {
                    // Reset Views and their texts/colors
                    setDefaultViews();
                    stopService(new Intent(getApplicationContext(), CountDownService.class));
                    progress = 0;
                    progressBar.clearAnimation();
                    progressBar.setProgress(progress);
                    handler.removeCallbacksAndMessages(null);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(br, new IntentFilter(CountDownService.BROADCAST_COUNTDOWN));
        // If service is already running set appropriate values for views
        checkPreferences();
        if(isMyServiceRunning(CountDownService.class)){
            startBtn.setText(R.string.stop_button);
            timerUnitView.setText(R.string.timer_unit);
            setViews(currentStatus);
            justIn = true; //flag for updating ui
        } else{
            setDefaultViews();
        }
        Log.i(TAG, "Registered broacast receiver");
    }

    /**
     * Reset Views to default values
     */
    public void setDefaultViews(){
        progress = 0;
        currentStatus = sitColor;
        statusView.setImageDrawable(sit);
        // Indicate what the timer number means
        statusTextView.setText("Timer set for " + timePeriodSit / MINUTE + " min");
        statusTextView.setTextColor(sitColor);
        // Timer unit (min remaining)
        timerUnitView.setText(R.string.number_picker_unit);
        timerView.setText("" + timePeriodSit / MINUTE);
        timerView.setTextColor(sitColor);
        timerUnitView.setTextColor(sitColor);
        // Button
        startBtn.setText(R.string.start_button);
    }

    /**
     * Updates settings fetched from the SharedPreferences
     */
    private void checkPreferences(){

        // Grab timer and sound settings
        int sittingPeriod = ((prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                SettingsActivity.SITTING_DEFAULT_VALUE) * 5) + 20) * MINUTE;

        int standingPeriod = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                SettingsActivity.STANDING_DEFAULT_VALUE)) * MINUTE;

        if(timePeriodSit != sittingPeriod){
            timePeriodSit = sittingPeriod;
            progressBar.setMax(timePeriodSit);
            Log.d(TAG, "progress max: "+progressBar.getMax());
        }

        if (timePeriodStand != standingPeriod){
            timePeriodStand = standingPeriod;
        }

//        Log.d("Resume", "sit: " + timePeriodSit);
//        Log.d("Resume", "stand: " + timePeriodStand);
//        Log.d("Resume", "sound: " + sound);
    }

    /**
     * BroadcastReceive for the CountDownTimerService
     */
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent); // or whatever method used to update your GUI fields
        }
    };

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            int timer = intent.getIntExtra(CountDownService.EXTRA_TIMER, 0);
            remainingMillis = intent.getLongExtra(CountDownService.EXTRA_COUNTDOWN, 0);

            // If there are secs left for the minute, display the minute
            // Ex: 2:50 should be registered as 2 min not 1 min
            int min = (int) remainingMillis / MINUTE;
            min += (remainingMillis - (min*MINUTE)) > 0 ? 1: 0;

            // Set the timer value
            if(remainingMillis <= 30000) {
                if(timer == sitColor) {
                    statusTextView.setText(R.string.ready_to_stand);
                }
                timerView.setText("< 1");
            } else if(min >= 1 && min < 10){
                timerView.setText(String.format("%01d", min));
            } else if(remainingMillis >= 10){
                timerView.setText(String.format("%02d", min));
            }

            // Set the proper views and update currentStatus so it can be saved if activity's destroyed
            if(timer == sitColor){
                currentStatus = sitColor;
                if(remainingMillis / MILLIS == 1){
                    transition = true;
                    setViews(standColor);
                    Log.i(TAG, "Finishing countdown for " + timer);
                    progress = timePeriodSit;
                    updateUI.run();
                }
            }  else{
                currentStatus = standColor;
                if(remainingMillis / MILLIS == 1){
                    transition = true;
                    setViews(sitColor);
                    progress = timePeriodStand;
                    updateUI.run();
                    Log.i(TAG, "Finishing countdown for " + timer);
                }
            }

//            Log.d("modulo","modulo val: "+(remainingMillis % MINUTE));

            // Only update progress bar every min for efficiency and update if resuming
            if(remainingMillis % MINUTE < 1000) {
                updateProgress(currentStatus);
            } else if(justIn){
                updateProgress(currentStatus);
                justIn = false;
            }

        }
    }

    /**
     * Convenience function to check whether or not the CountDownService is currently running
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience function for timers to set the views' text or color depending on whether it's
     * time to sit/stand.
     * @param color
     */
    public void setViews(int color){

        if(color == sitColor){
            timerView.setTextColor(sitColor);
            timerUnitView.setTextColor(sitColor);
            statusTextView.setTextColor(sitColor);
            statusTextView.setText(R.string.time_to_sit);
            statusView.setImageDrawable(sit);
            bgShape.setColor(sitColor);
            // Stay on standColor if we're just switching over to sitting for the rest of the
            // orange progress
            if(transition)
                bgShape.setColor(standColor);
        } else {
            timerView.setTextColor(standColor);
            timerUnitView.setTextColor(standColor);
            statusTextView.setTextColor(standColor);
            statusTextView.setText(R.string.time_to_stand);
            statusView.setImageDrawable(stand);
            bgShape.setColor(standColor);
            // Same as above
            if(transition)
                bgShape.setColor(sitColor);
        }

    }

    /**
     * Restore the progress on the progress bar
     * @param color
     */
    public void updateProgress(int color){

        if(color == sitColor){
            progress = timePeriodSit - (int) remainingMillis;
        } else{
            progress = timePeriodStand - (int) remainingMillis;
        }

//        Log.d("Progress", "amount to progress: "+progress);

        // Run animation in a different thread
        updateUI.run();

    }

    private Runnable updateUI = new Runnable() {
        @Override
        public void run() {
            // Moves the current Thread into the background
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progress); // see this max value coming back here, we animale towards that value
            animation.setDuration(1000); //in milliseconds
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (transition) {
                        transition = false;
                        progressBar.setProgress(0);
                        // Switch the color back to the current status
                        if (currentStatus == sitColor) {
                            bgShape.setColor(standColor);
                            progressBar.setMax(timePeriodStand);
//                            Log.d("max", "color: " + "sitColor" + " max: " + progressBar.getMax());
                        }
                        if (currentStatus == standColor) {
                            bgShape.setColor(sitColor);
                            progressBar.setMax(timePeriodSit);
//                            Log.d("max", "color: " + "standColor" + " max: " + progressBar.getMax());
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.settings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(br);
        Log.i(TAG, "Unregistered broacast receiver");
    }

    @Override
    protected void onStop(){
        super.onStop();
        try {
            unregisterReceiver(br);
        } catch (Exception e) {
            // Receiver was probably already stopped in onPause()
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, CountDownService.class));
        Log.i(TAG, "Stopped service");
    }
}