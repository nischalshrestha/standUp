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
import android.util.Log;

import com.bitsorific.standup.activity.MainActivity;
import com.bitsorific.standup.activity.SettingsActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sets up CountDownTimers for sitting and standing based on timePeriodSit and timePeriodStand
 * set with the default values or values set in preferences.
 *
 * Created by nischal on 2/11/16.
 */
public class CountDownService extends Service {

    private static final String TAG = "CountDownService";
    public static final String BROADCAST_COUNTDOWN = "com.bitsorific.standup.countdown_update";
    public static final String TYPE_SIT = "sitTimer";
    public static final String TYPE_STAND = "standTimer";

    private Handler handler = new Handler();
    private CountDownTimer sitTimer;
    private CountDownTimer standTimer;
    private Handler mhandler = new Handler();

    private Intent i = new Intent(BROADCAST_COUNTDOWN);

    private Boolean sound = false;
    private MediaPlayer mpAlarmStand;
    private MediaPlayer mpAlarmSit;
    private Vibrator v;

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            if(mpAlarmStand != null && mpAlarmStand.isPlaying()) {
                mpAlarmStand.stop();
            }
            if(mpAlarmSit != null && mpAlarmSit.isPlaying()) {
                mpAlarmSit.stop();
            }
        }
    };

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
                long ringLength = mpAlarmStand.getDuration();
                mpAlarmStand.start();
                new Timer().schedule(task, ringLength - 800);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable soundAlertSit = new Runnable() {
        @Override
        public void run() {
            try {
                mpAlarmSit.stop();
                mpAlarmSit.prepare();
                long ringLength = mpAlarmSit.getDuration();
                mpAlarmSit.start();
                new Timer().schedule(task, ringLength - 800);
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
        int sittingPeriod = prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                SettingsActivity.SITTING_DEFAULT_VALUE) * MainActivity.MINUTE;
        int standingPeriod = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                SettingsActivity.STANDING_DEFAULT_VALUE)) * MainActivity.MINUTE;

        Log.d("Resume", "sit: " + sittingPeriod);
        Log.d("Resume", "stand: " + standingPeriod);

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

        sitTimer = new CountDownTimer(60000+MainActivity.MILLIS, MainActivity.MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Countdown seconds remaining for sitTimer: " + millisUntilFinished / MainActivity.MILLIS);
                i.putExtra("timer", TYPE_SIT);
                i.putExtra("countdown", millisUntilFinished);
                sendBroadcast(i);
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "sitTimer finished");
                if(!sound) {
                    mhandler.post(vibrateAlert);
                } else {
                    Log.i(TAG, "int sit timer sound");
                    mhandler.post(soundAlertStand);
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        standTimer.start();
                    }
                }, MainActivity.MILLIS);
                Log.i(TAG, "Starting standTimer");
            }
        };

        standTimer = new CountDownTimer(60000+MainActivity.MILLIS, MainActivity.MILLIS){
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Countdown seconds remaining for standTimer: " + millisUntilFinished / MainActivity.MILLIS);
                i.putExtra("timer", TYPE_STAND);
                i.putExtra("countdown", millisUntilFinished);
                sendBroadcast(i);
            }

            // Set Views to sitting
            @Override
            public void onFinish() {
                Log.i(TAG, "standTimer finished");
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
                }, MainActivity.MILLIS);
                Log.i(TAG, "Starting sitTimer");
            }
        };

        sitTimer.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        sitTimer.cancel();
        standTimer.cancel();
        Log.i(TAG, "CountDownService cancelled");
        // Free sound resources
        if(mpAlarmStand != null) {
            mpAlarmStand.release();
            mpAlarmStand = null;
        }
        if(mpAlarmSit != null){
            mpAlarmSit.release();
            mpAlarmSit = null;
        }
        task.cancel();
        mhandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
