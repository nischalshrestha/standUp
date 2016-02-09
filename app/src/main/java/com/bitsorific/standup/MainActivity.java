package com.bitsorific.standup;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private TextView timerView;
    private TextView timerUnitView;

    private int timePeriodStand;
    private int timePeriodSit;

    private TextView statusTextView;
    private ImageView statusView;
    private Button startBtn;
    private Handler handler = new Handler();

    private CountDownTimer sitTimer;
    private CountDownTimer standTimer;

    private boolean isSitTimerRunning = false;
    private boolean isStandTimerRunning = false;

    private static Integer standColor;
    private static Integer sitColor;

    private Runnable vibrateAlert = new Runnable() {
        int count = 0;
        @Override
        public void run() {
            if (++count <= 3) {
                final Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                v.vibrate(100);
                handler.postDelayed(this, 500);
            } else {
                count = 0;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerView = (TextView) findViewById(R.id.timer);
        timerUnitView = (TextView) findViewById(R.id.timerUnit);
        statusView = (ImageView) findViewById(R.id.status);
        statusTextView = (TextView) findViewById(R.id.statusText);

        Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/Roboto-Thin.ttf");
        timerView.setTypeface(typeFace);

        sitColor = getResources().getColor(R.color.cyan);
        standColor = getResources().getColor(R.color.orange);

        // TODO Acquire from settings
        timePeriodSit = 60000;
        timePeriodStand = 30000;
        setUpTimers();

        // Button to start and stop session
        startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startBtn.getText().equals(getString(R.string.start_button))){
                    // Start the sit timer with default (sit) Views and text/color
                    statusView.setImageResource(R.drawable.sit_cyan);
                    timerView.setTextColor(sitColor);
                    timerUnitView.setTextColor(sitColor);
                    startBtn.setText(R.string.stop_button);
                    sitTimer.start();
                } else if(startBtn.getText().equals(getString(R.string.stop_button))){
                    // Reset Views and their texts/colors
                    statusTextView.setText("");
                    statusView.setImageResource(R.drawable.sit_cyan);
                    timerView.setText(R.string.reset_timer);
                    timerView.setTextColor(sitColor);
                    timerUnitView.setTextColor(sitColor);
                    startBtn.setText(R.string.start_button);
                    // Stop running timers
                    if(isSitTimerRunning) {
                        sitTimer.cancel();
                    } else if(isStandTimerRunning){
                        standTimer.cancel();;
                    }
                }
            }
        });

    }

    /**
     * Sets up CountDownTimers for sitting and standing based on timePeriodSit and timePeriodStand
     * set with the default values or from user preferences.
     */
    private void setUpTimers(){

        sitTimer = new CountDownTimer(timePeriodSit, 1000) {

            public void onTick(long millisUntilFinished) {
                isSitTimerRunning = true;
                timerView.setText(
                        String.format("%02d", millisUntilFinished / 60000) + ":" +
                                String.format("%02d", (int) (millisUntilFinished / 1000) % 60));
            }

            public void onFinish() {
                isSitTimerRunning = false;
                handler.post(vibrateAlert);
                setViews(standColor);
                standTimer.start();
            }
        };

        standTimer = new CountDownTimer(timePeriodStand, 1000){
            @Override
            public void onTick(long millisUntilFinished) {
                isStandTimerRunning = true;
                timerView.setText(
                        String.format("%02d", millisUntilFinished / 60000) + ":" +
                                String.format("%02d", (int) (millisUntilFinished / 1000) % 60));
            }
            @Override
            public void onFinish() {
                isStandTimerRunning = false;
                handler.post(vibrateAlert);
                setViews(sitColor);
                sitTimer.start();
            }
        };

    }

    /**
     * Sets the views' text or color depending on whether it's time to sit/stand.
     * @param color
     */
    public void setViews(Integer color){

        if(color.equals(sitColor)){
            statusTextView.setTextColor(sitColor);
            statusTextView.setText(R.string.time_to_sit);
            statusView.setImageResource(R.drawable.sit_cyan);
            timerView.setText(R.string.reset_timer);
            timerView.setTextColor(sitColor);
            timerUnitView.setTextColor(sitColor);
        } else{
            statusTextView.setTextColor(standColor);
            statusTextView.setText(R.string.time_to_stand);
            statusView.setImageResource(R.drawable.stand_orange);
            timerView.setText(R.string.reset_timer);
            timerView.setTextColor(standColor);
            timerUnitView.setTextColor(standColor);
        }

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
