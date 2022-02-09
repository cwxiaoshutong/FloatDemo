package com.cwxiaos.floatdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.security.Permission;

public class MainActivity extends AppCompatActivity {
    public static boolean blockScreenshots = true;
    public static boolean keepScreenOn = true;
    private PermissionUtil permissionUtil;

    private Intent intentFloatWindow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appEntry();
    }

    private void appEntry() {
        setupButtonListener();
        permissionUtil = new PermissionUtil();
    }

    private void setupButtonListener() {
        TextView buttonStart = findViewById(R.id.button_start);
        TextView buttonBlockScreenshots = findViewById(R.id.button_screenshot);
        TextView buttonKeepScreenOn = findViewById(R.id.button_screen_on);

        buttonBlockScreenshots.setTextColor(Color.GREEN);
        buttonKeepScreenOn.setTextColor(Color.GREEN);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (permissionUtil.isPermissionGranted(MainActivity.this)) {
                    if (intentFloatWindow != null) {
                        stopService(intentFloatWindow);
                        intentFloatWindow = null;
                    } else {
                        Log.d(getClass()+"//setupButtonListener()","Start Floating Window Service");
                        intentFloatWindow = new Intent(MainActivity.this, FloatViewService.class);
                        startForegroundService(intentFloatWindow);
                    }
                } else {
                    permissionUtil.checkPermission(MainActivity.this);
                }
            }
        });

        buttonBlockScreenshots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonBlockScreenshots.getText().equals("Block Screenshot:ENABLE")) {
                    buttonBlockScreenshots.setText("Block Screenshot:DISABLE");
                    buttonBlockScreenshots.setTextColor(Color.RED);
                    blockScreenshots = false;
                } else {
                    buttonBlockScreenshots.setText("Block Screenshot:ENABLE");
                    buttonBlockScreenshots.setTextColor(Color.GREEN);
                    blockScreenshots = true;
                }
            }
        });

        buttonKeepScreenOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonKeepScreenOn.getText().equals("Keep Screen On:ENABLE")) {
                    buttonKeepScreenOn.setText("Keep Screen On:DISABLE");
                    buttonKeepScreenOn.setTextColor(Color.RED);
                    keepScreenOn = false;
                } else {
                    buttonKeepScreenOn.setText("Keep Screen On:ENABLE");
                    buttonKeepScreenOn.setTextColor(Color.GREEN);
                    keepScreenOn = true;
                }
            }
        });
    }
}