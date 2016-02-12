package com.bitsorific.standup.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitsorific.standup.R;
import com.bitsorific.standup.service.CountDownService;


public class MainActivity extends AppCompatActivity {

    public static final int MINUTE = 60000;
    public static final int MILLIS = 1000;

    private static final String TAG = "MainActivity";
    private TextView timerView;
    private TextView timerUnitView;

    private int timePeriodStand;
    private int timePeriodSit;

    private TextView statusTextView;
    private ImageView statusView;
    private Button startBtn;

    private static Integer standColor;
    private static Integer sitColor;

    private Drawable sit;
    private Drawable stand;

    private SharedPreferences prefs;

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

        // Grab timer and sound settings
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Log.d(TAG, "onCreate() called");

        // Set up Button to start and stop session
        startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startBtn.getText().equals(getString(R.string.start_button))) {
                    // Sit image and status text above sit image
                    statusView.setImageDrawable(sit);
                    // Timer text
                    timerView.setTextColor(sitColor);
                    timerUnitView.setText(R.string.timer_unit);
                    timerUnitView.setTextColor(sitColor);
                    // Button
                    startBtn.setText(R.string.stop_button);
                    startService(new Intent(getApplicationContext(), CountDownService.class));
                    Log.i(TAG, "Started service");
//                    sitTimer.start();
                } else if (startBtn.getText().equals(getString(R.string.stop_button))) {
                    // Reset Views and their texts/colors
                    statusView.setImageDrawable(sit);
                    statusTextView.setText("Timer set for " + timePeriodSit / MINUTE + " min");
                    statusTextView.setTextColor(sitColor);
                    // Timer
                    timerUnitView.setText(R.string.number_picker_unit);
                    timerView.setText("" + timePeriodSit / MINUTE);
                    timerView.setTextColor(sitColor);
                    timerUnitView.setTextColor(sitColor);
                    // Button
                    startBtn.setText(R.string.start_button);
                    stopService(new Intent(getApplicationContext(), CountDownService.class));
                    Log.i(TAG, "in");
                }
            }
        });

    }

    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent); // or whatever method used to update your GUI fields
        }
    };

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            String timer = intent.getStringExtra("timer");
            long rem = intent.getLongExtra("countdown", 0);

            // Ex: if 2:50 don't register 1 min
            int min = (int) rem / MINUTE;
            if(rem - (min*MINUTE) > 0){
                min++;
            }

            if(rem <= 30000) {
                if(timer.equals(CountDownService.TYPE_SIT)) {
                    statusTextView.setText(R.string.ready_to_stand);
                }
                timerView.setText("< 1");
            } else if(min >= 1 && min < 10){
                timerView.setText(String.format("%01d", min));
            } else if(rem >= 10){
                timerView.setText(String.format("%02d", min));
            }

            if(timer.equals(CountDownService.TYPE_SIT)){
                if(rem == timePeriodSit){
                    Log.i(TAG, "Broadcasts receiving from: " + timer);
                }
                if(rem / MILLIS == 1){
                    setViews(standColor);
                    Log.i(TAG, "Finishing countdown for " + timer);
                } else if(rem / MILLIS > 1 && timerView.getText().equals("")){
                    setViews(sitColor);
                }
            }  else{
                if(rem == timePeriodStand){
                    Log.i(TAG, "Broadcasts receiving from: " + timer);
                }
                if(rem / MILLIS == 1){
                    setViews(sitColor);
                    Log.i(TAG, "Finishing countdown for " + timer);
                } else if(rem / MILLIS > 1 && timerView.getText().equals("")){
                    setViews(standColor);
                }
            }

            Log.i(TAG, "Countdown seconds remaining for " + timer + ": " +  rem / MILLIS);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        // TODO Check if settings have changed
        checkPreferences();
        Log.d(TAG, "onResume() called");
        registerReceiver(br, new IntentFilter(CountDownService.BROADCAST_COUNTDOWN));
        Log.i(TAG, "Registered broacast receiver");
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void checkPreferences(){

        // Grab timer and sound settings
        int sittingPeriod = ((prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                SettingsActivity.SITTING_DEFAULT_VALUE) * 5) + 20) * MINUTE;

        int standingPeriod = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                SettingsActivity.STANDING_DEFAULT_VALUE)) * MINUTE;

        if(timePeriodSit != sittingPeriod){
            timePeriodSit = sittingPeriod;
        }

        if(timePeriodStand != standingPeriod){
            timePeriodStand = standingPeriod;
        }

        // Set up CountdownTimers
        if(!isMyServiceRunning(CountDownService.class)) {
            // Set initial text
            timerView.setText(""+timePeriodSit / MINUTE);
            statusTextView.setText("Timer set for "+ timePeriodSit / MINUTE + " min");
            timerUnitView.setText(R.string.number_picker_unit);
        } else{
            Log.d(TAG, "service is running!");
        }

//        Log.d("Resume", "sit: " + timePeriodSit);
//        Log.d("Resume", "stand: " + timePeriodStand);
//        Log.d("Resume", "sound: " + sound);
    }

    /**
     * Convenience function for timers to set the views' text or color depending on whether it's
     * time to sit/stand.
     * @param color
     */
    public void setViews(Integer color){

        if(color.equals(sitColor)){
            timerView.setTextColor(sitColor);
            timerUnitView.setTextColor(sitColor);
            statusTextView.setTextColor(sitColor);
            statusTextView.setText(R.string.time_to_sit);
            statusView.setImageDrawable(sit);
        } else {
            timerView.setTextColor(standColor);
            timerUnitView.setTextColor(standColor);
            statusTextView.setTextColor(standColor);
            statusTextView.setText(R.string.time_to_stand);
            statusView.setImageDrawable(stand);
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