package com.bitsorific.standup.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
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
import com.bitsorific.standup.preference.NumberPickerPreference;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {


    private static final int MINUTE = 60000;
    private TextView timerView;
    private TextView timerUnitView;

    private int timePeriodStand;
    private int timePeriodSit;

    private TextView statusTextView;
    private ImageView statusView;
    private Button startBtn;

    private Handler handler = new Handler();
    private Handler mhandler = new Handler();

    private CountDownTimer sitTimer;
    private CountDownTimer standTimer;

    private boolean isSitTimerRunning = false;
    private boolean isStandTimerRunning = false;

    private static Integer standColor;
    private static Integer sitColor;

    private Drawable sit;
    private Drawable stand;

    private Boolean sound = false;
    private MediaPlayer mpAlarmStand;
    private MediaPlayer mpAlarmSit;
    private Vibrator v;

    private Runnable vibrateAlert = new Runnable() {
        int count = 0;
        @Override
        public void run() {
            if (++count <= 3) {
                // Vibrate for 500 milliseconds
                v.vibrate(100);
                mhandler.postDelayed(this, 100);
            } else {
                count = 0;
            }
        }
    };

    private Runnable soundAlertStand = new Runnable() {
        @Override
        public void run() {
            try {
                mpAlarmStand.stop();
                mpAlarmStand.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            long ringLength = mpAlarmStand.getDuration();
            mpAlarmStand.start();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mpAlarmStand.stop();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, ringLength - 800);
        }
    };

    private Runnable soundAlertSit = new Runnable() {
        @Override
        public void run() {
            try {
                mpAlarmSit.stop();
                mpAlarmSit.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            long ringLength = mpAlarmSit.getDuration();
            mpAlarmSit.start();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mpAlarmSit.stop();
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, ringLength - 800);
        }
    };

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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int sittingPeriod = prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                NumberPickerPreference.SITTING_DEFAULT_VALUE) * MINUTE;
        int standingPeriod = prefs.getInt(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                NumberPickerPreference.STANDING_DEFAULT_VALUE) * MINUTE;

        timePeriodSit = sittingPeriod != timePeriodSit ? sittingPeriod : timePeriodSit;
        timePeriodStand = standingPeriod != timePeriodStand ? standingPeriod : timePeriodStand;

        // Set initial text
        timerView.setText(""+timePeriodSit / MINUTE);
        statusTextView.setText("Timer set for "+ timePeriodSit / MINUTE + " min");
        timerUnitView.setText(R.string.number_picker_unit);

        // Set up CountdownTimers
        setUpTimers();

        // Set up vibrator and/or sound
        v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        sound = prefs.getBoolean(SettingsActivity.KEY_PREF_SOUND, SettingsActivity.PREF_SOUND_DEFAULT);
        if(sound){
            String uriStand = prefs.getString(SettingsActivity.KEY_PREF_ALARM_TONE_STAND,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            String uriSit = prefs.getString(SettingsActivity.KEY_PREF_ALARM_TONE_SIT,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            mpAlarmStand = MediaPlayer.create(getApplicationContext(), Uri.parse(uriStand));
            mpAlarmSit =  MediaPlayer.create(getApplicationContext(), Uri.parse(uriSit));
        }

        Log.d("Resume", "sit: "+timePeriodSit);
        Log.d("Resume", "stand: "+timePeriodStand);
        Log.d("Resume", "sound: "+sound);

        // Set up Button to start and stop session
        startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startBtn.getText().equals(getString(R.string.start_button))) {
                    // Sit image and status text above sit image
                    statusView.setImageDrawable(sit);
                    statusTextView.setText(R.string.session_started);
                    // Timer text
                    timerView.setTextColor(sitColor);
                    timerUnitView.setText(R.string.timer_unit);
                    timerUnitView.setTextColor(sitColor);
                    // Button
                    startBtn.setText(R.string.stop_button);
                    sitTimer.start();
                } else if (startBtn.getText().equals(getString(R.string.stop_button))) {
                    // Reset Views and their texts/colors
                    statusView.setImageDrawable(sit);
                    statusTextView.setText("Timer set for " + timePeriodSit / MINUTE);
                    // Timer
                    timerUnitView.setText(R.string.number_picker_unit);
                    timerView.setText(""+timePeriodSit / MINUTE);
                    timerView.setTextColor(sitColor);
                    timerUnitView.setTextColor(sitColor);
                    // Button
                    startBtn.setText(R.string.start_button);
                    // Stop running timers
                    if (isSitTimerRunning) {
                        sitTimer.cancel();
                    } else if (isStandTimerRunning) {
                        standTimer.cancel();
                    }
                }
            }
        });

    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    /**
     * Sets up CountDownTimers for sitting and standing based on timePeriodSit and timePeriodStand
     * set with the default values or values set in preferences
     */
    private void setUpTimers(){

        sitTimer = new CountDownTimer(timePeriodSit+1000, 60000) {

            public void onTick(long millisUntilFinished) {
                isSitTimerRunning = true;
                Log.d("Timer", "ms until finished: " + (int) millisUntilFinished / 1000);
                long left = millisUntilFinished / 60000;
                if(left <= 1) {
                    statusTextView.setText(R.string.ready_to_stand);
                    timerView.setText(String.format("< %01d", millisUntilFinished / 60000));
                } else if(left < 10){
                    timerView.setText(String.format("%01d", millisUntilFinished / 60000));
                } else if(left >= 10){
                    timerView.setText(String.format("%02d", millisUntilFinished / 60000));
                }
            }

            // Set Views to standing
            @Override
            public void onFinish() {
                isSitTimerRunning = false;
                setViews(standColor);
                if(!sound) {
                    mhandler.post(vibrateAlert);
                } else {
                    mhandler.post(soundAlertStand);
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        standTimer.start();
                    }
                }, 1000);
            }
        };

        standTimer = new CountDownTimer(timePeriodStand+1000, 60000){
            @Override
            public void onTick(long millisUntilFinished) {
                isStandTimerRunning = true;
                long left = millisUntilFinished / 60000;
                if(left <= 1) {
                    timerView.setText(String.format("< %01d", millisUntilFinished / 60000));
                } else if(left < 10){
                    timerView.setText(String.format("%01d", millisUntilFinished / 60000));
                } else if(left >= 10){
                    timerView.setText(String.format("%02d", millisUntilFinished / 60000));
                }
            }

            // Set Views to sitting
            @Override
            public void onFinish() {
                isStandTimerRunning = false;
                setViews(sitColor);
                if(!sound) {
                    mhandler.post(vibrateAlert);
                } else{
                    mhandler.post(soundAlertSit);
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sitTimer.start();
                    }
                }, 1000);
            }
        };

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
        // Stop running timers
        if(isSitTimerRunning) {
            sitTimer.cancel();
        } else if(isStandTimerRunning){
            standTimer.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        mhandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Free sound resources
        if(mpAlarmStand != null) {
            mpAlarmStand.release();
            mpAlarmStand = null;
        }
        handler.removeCallbacksAndMessages(null);
        mhandler.removeCallbacksAndMessages(null);
    }
}