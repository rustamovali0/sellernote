package com.rustamoff.jeans;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 15, 24));

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.rustamoff.jeans.R.drawable.splash_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);

        int size = (int) (300 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        root.addView(logo, params);

        setContentView(root);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1000);
    }
}
