package com.bitsorific.standup.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitsorific.standup.R;


public class MainActivity extends AppCompatActivity {

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

        v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        sit = getResources().getDrawable(R.drawable.ic_seat_recline_normal);
        stand = getResources().getDrawable(R.drawable.ic_walk);

        // TODO Acquire from settings
        timePeriodSit = 120000;
        timePeriodStand = 60000;
        setUpTimers();

        // Button to start and stop session
        startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(startBtn.getText().equals(getString(R.string.start_button))){
                // Start the sit timer with default (sit) Views and text/color
                statusView.setImageDrawable(sit);
                timerView.setTextColor(sitColor);
                timerUnitView.setTextColor(sitColor);
                startBtn.setText(R.string.stop_button);
                sitTimer.start();
            } else if(startBtn.getText().equals(getString(R.string.stop_button))){
                // Reset Views and their texts/colors
                statusTextView.setText("");
                statusView.setImageDrawable(sit);
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

    @Override
    protected void onResume(){
        super.onResume();
    }

    /**
     * Sets up CountDownTimers for sitting and standing based on timePeriodSit and timePeriodStand
     * set with the default values or from user preferences.
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

            public void onFinish() {
                isSitTimerRunning = false;
//                handler.post(vibrateAlert);
                setViews(standColor);
                vibrateAlert.run();
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
            @Override
            public void onFinish() {
                isStandTimerRunning = false;
                setViews(sitColor);
                vibrateAlert.run();
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
     * Sets the views' text or color depending on whether it's time to sit/stand.
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
