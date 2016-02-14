package com.bitsorific.standup.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bitsorific.standup.activity.MainActivity;
import com.bitsorific.standup.activity.SettingsActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This Service class sets up CountDownTimers for sitting and standing based on timePeriodSit and
 * timePeriodStand set with the default values or user set from preferences.
 *
 * Created by nischal on 2/11/16.
 */
public class CountDownService extends Service {

    // Constants
    private static final String TAG = "CountDownService";
    public static final String BROADCAST_COUNTDOWN = "com.bitsorific.standup.countdown_update";
    public static final String EXTRA_TIMER = "timer";
    public static final String EXTRA_COUNTDOWN = "countdown";

    // Timers
    private Timer timer;
    private CountDownTimer sitTimer;
    private CountDownTimer standTimer;
    private Handler mhandler = new Handler();

    // Intent for broadcasting
    private Intent i = new Intent(BROADCAST_COUNTDOWN);

    // Sound/vibrator
    private Boolean sound = false;
    private MediaPlayer mpAlarmStand;
    private MediaPlayer mpAlarmSit;
    private Vibrator v;

    /**
     * Timer to cancel media players after playing its length
     */
    private class CancelAlarm extends TimerTask {
        @Override
        public void run() {
            // Do stuff
            if(mpAlarmStand != null && mpAlarmStand.isPlaying()) {
                mpAlarmStand.stop();
            }
            if(mpAlarmSit != null && mpAlarmSit.isPlaying()) {
                mpAlarmSit.stop();
            }
        }
    }

    /**
     * Vibrate 3 times (TODO: ALLOW MORE OPTIONS LIKE PULSE AND PULSE SPEED)
     */
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

    /**
     * Alarm tone for standing up
     */
    private Runnable soundAlertStand = new Runnable() {
        @Override
        public void run() {
            try {
                mpAlarmStand.stop();
                mpAlarmStand.prepare();
                long ringLength = mpAlarmStand.getDuration();
                mpAlarmStand.start();
                timer = new Timer("cancel", true);
                CancelAlarm cancelAlarm = new CancelAlarm();
                timer.schedule(cancelAlarm, ringLength - 800);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Alarm tone for sitting back down
     */
    private Runnable soundAlertSit = new Runnable() {
        @Override
        public void run() {
            try {
                mpAlarmSit.stop();
                mpAlarmSit.prepare();
                long ringLength = mpAlarmSit.getDuration();
                mpAlarmSit.start();
                timer = new Timer("cancel", true);
                CancelAlarm cancelAlarm = new CancelAlarm();
                timer.schedule(cancelAlarm, ringLength - 800);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Starting timer...");

        // Grab timer and sound settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int sittingPeriod = ((prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                SettingsActivity.SITTING_DEFAULT_VALUE) * 1) + 1) * MainActivity.MINUTE;
        int standingPeriod = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                SettingsActivity.STANDING_DEFAULT_VALUE)) * MainActivity.MINUTE;

//        Log.d("Resume", "sit: " + sittingPeriod);
//        Log.d("Resume", "stand: " + standingPeriod);

        // Check sound
        v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        sound = prefs.getBoolean(SettingsActivity.KEY_PREF_SOUND, SettingsActivity.PREF_SOUND_DEFAULT);
        if(sound){
            String uriStand = prefs.getString(SettingsActivity.KEY_PREF_ALARM_TONE_STAND,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            String uriSit = prefs.getString(SettingsActivity.KEY_PREF_ALARM_TONE_SIT,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            // check if they're running first
            mpAlarmStand = MediaPlayer.create(getApplicationContext(), Uri.parse(uriStand));
            mpAlarmSit =  MediaPlayer.create(getApplicationContext(), Uri.parse(uriSit));
        }

        // Sitting down timer
        sitTimer = new CountDownTimer(sittingPeriod, MainActivity.MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
//                Log.i(TAG, "Countdown seconds remaining for sitTimer: " + millisUntilFinished % MainActivity.MINUTE);
                i.putExtra(EXTRA_TIMER, MainActivity.sitColor);
                i.putExtra(EXTRA_COUNTDOWN, millisUntilFinished);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }

            @Override
            public void onFinish() {
//                Log.i(TAG, "sitTimer finished");
                if(!sound) {
                    mhandler.post(vibrateAlert);
                } else {
                    mhandler.post(soundAlertStand);
                }
                standTimer.start();
            }
        };

        // Standing up timer
        standTimer = new CountDownTimer(standingPeriod, MainActivity.MILLIS){
            @Override
            public void onTick(long millisUntilFinished) {
//                Log.i(TAG, "Countdown seconds remaining for standTimer: " + millisUntilFinished % MainActivity.MINUTE);
                i.putExtra(EXTRA_TIMER, MainActivity.standColor);
                i.putExtra(EXTRA_COUNTDOWN, millisUntilFinished);
                sendBroadcast(i);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }

            // Set Views to sitting
            @Override
            public void onFinish() {
//                Log.i(TAG, "standTimer finished");
                if(!sound) {
                    mhandler.post(vibrateAlert);
                } else{
                    mhandler.post(soundAlertSit);
                }
                sitTimer.start();
//                Log.i(TAG, "Starting sitTimer");
            }
        };

        sitTimer.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        // Cancel timers
        sitTimer.cancel();
        standTimer.cancel();
        Log.i(TAG, "CountDownService cancelled");
        // Free mp resources and any messages on handler
        if(mpAlarmStand != null) {
            mpAlarmStand.release();
            mpAlarmStand = null;
        }
        if(mpAlarmSit != null){
            mpAlarmSit.release();
            mpAlarmSit = null;
        }
        if(timer != null) {
            timer.cancel();
        }
        mhandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
