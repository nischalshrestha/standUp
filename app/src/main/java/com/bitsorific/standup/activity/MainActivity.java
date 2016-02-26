package com.bitsorific.standup.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.PendingIntent;
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
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.Toast;

import com.bitsorific.standup.R;
import com.bitsorific.standup.service.CountDownService;
import com.bitsorific.standup.service.DetectedActivitiesIntentService;
import com.bitsorific.standup.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

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
    public static int startBtnColor;
    public static int stopBtnColor;

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

    // Flags to update progress UI when resuming activity and switching states
    private boolean justIn = false;
    private boolean transition = false;

    // Intent for broadcasting
    private IntentFilter filter = new IntentFilter(CountDownService.BROADCAST_COUNTDOWN);

    /**
     * A receiver for DetectedActivity objects broadcast by the
     * {@code ActivityDetectionIntentService}.
     */
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

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
//        statusTextView.setTypeface(typeFace);
        timerView.setTypeface(typeFace);
        timerUnitView.setTypeface(typeFace);

        // Set up vars for TextView colors and images
        sitColor = getResources().getColor(R.color.cyan);
        standColor = getResources().getColor(R.color.orange);
        startBtnColor = getResources().getColor(R.color.green);
        stopBtnColor = getResources().getColor(R.color.red);

        sit = getResources().getDrawable(R.drawable.ic_seat_recline_normal);
        stand = getResources().getDrawable(R.drawable.ic_walk);

        // Set up progress bar and button to start/stop timers
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        RotateDrawable rd = (RotateDrawable) progressBar.getProgressDrawable();
        bgShape = (GradientDrawable) rd.getDrawable();

        startBtn = (Button) findViewById(R.id.start);

        // Grab timer and sound settings
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        Log.d(TAG, "onCreate() called");

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startBtn.getText().equals(getString(R.string.start_button))) {
                    // Sit off service to start timer(s)
                    statusTextView.setText(R.string.time_to_sit);
                    timerUnitView.setText(R.string.timer_unit);
                    startBtn.setText(R.string.stop_button);
                    ((GradientDrawable) startBtn.getBackground()).setColor(stopBtnColor);
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
                    ((GradientDrawable) startBtn.getBackground()).setColor(startBtnColor);
                }
            }
        });

        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register receiver for local broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(br, filter);
        // Register the broadcast receiver that informs this activity of the DetectedActivity
        // object broadcast sent by the intent service.
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));

        checkPreferences();
        // If service is already running set appropriate values for views
        if(isMyServiceRunning(CountDownService.class)) {
            startBtn.setText(R.string.stop_button);
            timerUnitView.setText(R.string.timer_unit);
        } else{
            setDefaultViews();
        }
        justIn = true; //flag for updating ui
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
        ((GradientDrawable) startBtn.getBackground()).setColor(startBtnColor);
    }

    /**
     * Updates settings fetched from the SharedPreferences
     */
    private void checkPreferences(){

        // Grab timer and sound settings
        int sittingPeriod = ((prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                SettingsActivity.SITTING_DEFAULT_VALUE) * SettingsActivity.STANDING_MULTIPLE)
                + SettingsActivity.STANDING_MIN) * MINUTE;

        int standingPeriod = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                SettingsActivity.STANDING_DEFAULT_VALUE)) * MINUTE;

        if(timePeriodSit != sittingPeriod){
            timePeriodSit = sittingPeriod;
            progressBar.setMax(timePeriodSit);
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
                startBtn.setText(R.string.stop_button);
                ((GradientDrawable) startBtn.getBackground()).setColor(stopBtnColor);
                setViews(currentStatus);
                updateProgress(currentStatus);
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
            progressBar.setMax(timePeriodSit);
            progress = timePeriodSit - (int) remainingMillis;
        } else{
            progressBar.setMax(timePeriodStand);
            progress = timePeriodStand - (int) remainingMillis;
        }

        if(justIn){
//            progressBar.setProgress(0);
            justIn = false;
        }
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
                        }
                        if (currentStatus == standColor) {
                            bgShape.setColor(sitColor);
                            progressBar.setMax(timePeriodSit);
                        }
                    }
                    if(startBtn.getText().equals(getResources().getString(R.string.start_button))){
                        progressBar.setProgress(0);
                    }

                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
        }
    };

    /** Activity Recognition **/

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Registers for activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code requestActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} starts receiving callbacks when
     * activities are detected.
     */
    public void requestActivityUpdatesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
//            // Toggle the status of activity updates requested, and save in shared preferences.
//            boolean requestingUpdates = !getUpdatesRequestedState();
//            setUpdatesRequestedState(requestingUpdates);
//
//            // Update the UI. Requesting activity updates enables the Remove Activity Updates
//            // button, and removing activity updates enables the Add Activity Updates button.
//            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(true ? R.string.activity_updates_added :
                            R.string.activity_updates_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "activity-detection-response-receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
//            updateDetectedActivitiesList(updatedActivities);
        }
    }


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
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister receiver when activity is in the background
        LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        Log.i(TAG, "Unregistered broacast receiver");
    }

    @Override
    protected void onStop(){
        super.onStop();
        try {
            // Unregister receiver when activity is not visible
            LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
            mGoogleApiClient.disconnect();
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