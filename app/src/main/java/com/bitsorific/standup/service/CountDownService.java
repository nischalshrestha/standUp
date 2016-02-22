package com.bitsorific.standup.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.bitsorific.standup.R;
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

    // Used to access settings
    private SharedPreferences prefs;

    // Sound
    private Boolean sound = false;
    private Boolean notifications = false;

    private MediaPlayer mpAlarmStand;
    private MediaPlayer mpAlarmSit;

    // Vibrate
    private Vibrator v;
    private int pulseSpeed;
    private int pulseSpeedSit;
    private int pulseNum;
    private int pulseNumSit;

    private NotificationCompat.Builder mBuilder;
    private NotificationCompat.Builder mBuilderStand;

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
     * Vibrate x number of times given the number of pulses and the speed
     */
    private Runnable vibrateAlert = new Runnable() {
        int count = 0;
        @Override
        public void run() {
            if (++count <= pulseNum) {
                v.vibrate(pulseSpeed);
                // 100ms needs to be added for proper distance btw pulses
                mhandler.postDelayed(this, pulseSpeed+100);
            } else{
                count = 0;
            }
        }
    };

    /**
     * Vibrate x number of times given the number of pulses and the speed
     */
    private Runnable vibrateAlertSit = new Runnable() {
        int count = 0;
        @Override
        public void run() {
            if (++count <= pulseNumSit) {
                v.vibrate(pulseSpeedSit);
                // 100ms needs to be added for proper distance btw pulses
                mhandler.postDelayed(this, pulseSpeedSit+100);
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
                Log.d(TAG,"length of stand up ringtone: "+ringLength);
                mpAlarmStand.start();
                timer = new Timer("cancel", true);
                CancelAlarm cancelAlarm = new CancelAlarm();
                timer.schedule(cancelAlarm, ringLength);
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
                Log.d(TAG,"length of sit down ringtone: "+ringLength);
                mpAlarmSit.start();
                timer = new Timer("cancel", true);
                CancelAlarm cancelAlarm = new CancelAlarm();
                timer.schedule(cancelAlarm, ringLength);
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
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int sittingPeriod = ((prefs.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD,
                SettingsActivity.SITTING_DEFAULT_VALUE) * SettingsActivity.STANDING_MULTIPLE)
                + SettingsActivity.STANDING_MIN) * MainActivity.MINUTE;
        int standingPeriod = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_STANDING_PERIOD,
                SettingsActivity.STANDING_DEFAULT_VALUE)) * MainActivity.MINUTE;

//        Log.d("Pref", "sit: " + sittingPeriod);
//        Log.d("Pref", "stand: " + standingPeriod);

        // Check sound
        v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        checkPreferences();

        // Set up notifications
        mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_seat_recline_normal_white)
                        .setContentTitle(getString(R.string.notification_sit_reminder))
                        .setContentText(getString(R.string.notification_sit));

        mBuilderStand =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_walk_white)
                        .setContentTitle(getString(R.string.notification_stand_reminder))
                        .setContentText(getString(R.string.notification_stand));

        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilderStand.setContentIntent(resultPendingIntent);
        mBuilder.setContentIntent(resultPendingIntent);

        // Sets an ID for the notification
        final int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        final NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
                // Check for any changed setttings before playing sound/vibrate and showing
                // notification
                checkPreferences();
                if(!sound) {
                    mhandler.post(vibrateAlert);
                } else {
                    mhandler.post(soundAlertStand);
                }
                if(notifications) {
                    mNotifyMgr.notify(mNotificationId, mBuilderStand.build());
                } else{
                    mNotifyMgr.cancelAll();
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
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }

            // Set Views to sitting
            @Override
            public void onFinish() {
//                Log.i(TAG, "standTimer finished");

                // Check for any changed setttings before playing sound/vibrate and showing
                // notification
                checkPreferences();
                if(!sound) {
                    mhandler.post(vibrateAlertSit);
                } else{
                    mhandler.post(soundAlertSit);
                }
                sitTimer.start();
                if(notifications) {
                    mNotifyMgr.notify(mNotificationId, mBuilder.build());
                }
//                Log.i(TAG, "Starting sitTimer");
            }
        };

        sitTimer.start();
        if(notifications) {
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        } else{
            mNotifyMgr.cancelAll();
        }

    }

    /**
     * Check for any changes in user preference for notifications, vibrate, and sound settings
     */
    public void checkPreferences(){

        //Notification
        notifications = prefs.getBoolean(SettingsActivity.KEY_NOTIFICATIONS,
                SettingsActivity.PREF_NOTIFICATION_DEFAULT);

        // Sound
        sound = prefs.getBoolean(SettingsActivity.KEY_PREF_SOUND, SettingsActivity.PREF_SOUND_DEFAULT);
        if(sound){
            String uriStand = prefs.getString(SettingsActivity.KEY_PREF_ALARM_TONE_STAND,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL).toString());
            String uriSit = prefs.getString(SettingsActivity.KEY_PREF_ALARM_TONE_SIT,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL).toString());
            // check if they're running first
            // then set new MediaPlayers with possibly different ringtones.
            mpAlarmStand = MediaPlayer.create(getApplicationContext(), Uri.parse(uriStand));
            mpAlarmSit =  MediaPlayer.create(getApplicationContext(), Uri.parse(uriSit));
        }

        //Vibrate
        pulseNum = (prefs.getInt(SettingsActivity.KEY_PREF_PULSE_NUM,
                SettingsActivity.PULSE_NUM_DEFAULT_VALUE) * 1) + 1;
        pulseNumSit = (prefs.getInt(SettingsActivity.KEY_PREF_PULSE_NUM_SIT,
                SettingsActivity.PULSE_NUM_DEFAULT_VALUE_SIT) * 1) + 1;

        pulseSpeed = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_PULSE_SPEED,
                SettingsActivity.PULSE_SPEED_DEFAULT_VALUE));
        pulseSpeedSit =  Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_PULSE_SPEED_SIT,
                SettingsActivity.PULSE_SPEED_DEFAULT_VALUE));

//        Log.d("Pref", "vibrate num for stand: " + pulseNum);
//        Log.d("Pref", "vibrate num for sit: " + pulseNumSit);
//        Log.d("Pref", "vibrate speed for stand: " + pulseSpeed);
//        Log.d("Pref", "vibrate speed for sit: " + pulseSpeedSit);
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
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancelAll();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
