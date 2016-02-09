package com.bitsorific.standup;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private RelativeLayout bgView;
    private TextView timerView;
    private TextView statusTextView;
    private ImageView statusView;
    private Button startBtn;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerView = (TextView) findViewById(R.id.timer);
        statusView = (ImageView) findViewById(R.id.status);
        statusTextView = (TextView) findViewById(R.id.statusText);

        Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/Roboto-Thin.ttf");
        timerView.setTypeface(typeFace);
        statusTextView.setTypeface(typeFace);

        final int timePeriod = 60000;
        final CountDownTimer timer = new CountDownTimer(timePeriod, 1000) {

            public void onTick(long millisUntilFinished) {
                timerView.setText(
                        String.format("%02d", millisUntilFinished / 60000) + ":" +
                        String.format("%02d", (int) (millisUntilFinished / 1000) % 60) + " min");
            }

            public void onFinish() {
                statusTextView.setText(R.string.time_to_stand);
                statusView.setImageResource(R.drawable.stand);
                timerView.setText(R.string.reset_timer);
                handler.post(new Runnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        if(++count <= 3) {
                            final Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            v.vibrate(100);
                            handler.postDelayed(this, 500);
                        }
                    }
                });
            }

        };

        int colorFrom = getResources().getColor(R.color.startColor);
        int colorTo = getResources().getColor(R.color.endColor);
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(timePeriod); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                timerView.setTextColor((int) animator.getAnimatedValue());
                statusTextView.setTextColor((int) animator.getAnimatedValue());
            }
        });

        // Button to start and stop session
        startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startBtn.getText().equals(getString(R.string.start_button))){
                    statusView.setImageResource(R.drawable.sit);
                    startBtn.setText(R.string.stop_button);
                    timer.start();
                    colorAnimation.start();
                } else if(startBtn.getText().equals(getString(R.string.stop_button))){
                    statusView.setImageResource(R.drawable.sit);
                    timerView.setText(R.string.reset_timer);
                    startBtn.setText(R.string.start_button);
                    timer.cancel();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
