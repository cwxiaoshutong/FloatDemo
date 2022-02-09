package com.cwxiaos.floatdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import androidx.core.app.NotificationCompat;

public class FloatViewService extends Service {


    private WindowManager windowManager;
    private View floatingView;
    private float lastDirection = 0f;
    public float currentCompassDirection = 0f;
    public float currentJoystickDirection = 0f;
    public float currentProportion = 0f;


    private float compassRadius;
    private float joystickRadius;

    private View compassView;
    private View joystickView;
    private View pointerView;

    private final float longPressMaximumOffset = 4f;
    private float maximumOffset;
    private boolean isCompassAvailable = false;

    private SensorManager sensorManager;

    private final long lockJoystickDuration = 2000;

    /**
     * 处理指南针
     */
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            rotateCompass(event.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(getClass() + "//onAccuracyChanged()", sensor + ":" + accuracy);
        }
    };

    /**
     * 设置指南针方向
     *
     * @param degree 当前方向
     */
    private void rotateCompass(float degree) {
        isCompassAvailable = true;//如果有调用，则说明设备物理方向可用
        //更新对应的数据
        currentCompassDirection = currentJoystickDirection + lastDirection - 270;
        if (compassView != null && pointerView != null) {

            compassView.startAnimation(new RotateAnimation(-lastDirection, -degree,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f));
            pointerView.setRotation(0f - 90);
            lastDirection = degree;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupNotification();

        //getting the widget layout from xml using layout inflater
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, (ViewGroup) windowManager);

        //setting the layout parameters
        WindowManager.LayoutParams params;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        if (MainActivity.keepScreenOn) {//保持屏幕常亮
            flags = flags + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        if (MainActivity.blockScreenshots) {//阻止截屏
            flags = flags + WindowManager.LayoutParams.FLAG_SECURE;
        }

        //setting the layout types
        int types = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;//Android O以前可用
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            types = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                types,
                flags,//阻止截屏
                PixelFormat.TRANSLUCENT);


        //getting windows services and adding the floating view to it
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        View expandedView = floatingView.findViewById(R.id.layout_expanded);

        //注册指南针传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);//TODO:更换为新API


        //处理摇杆数据
        compassView = floatingView.findViewById(R.id.float_window_compass);
        joystickView = floatingView.findViewById(R.id.float_window_joystick);

        pointerView = floatingView.findViewById(R.id.float_window_pointer);

        compassRadius = getResources().getDimension(R.dimen.float_window_compass_size) / 2;
        joystickRadius = getResources().getDimension(R.dimen.float_window_joystick_size) / 2;
        joystickView.setY(compassRadius - joystickRadius);
        joystickView.setX(compassRadius - joystickRadius);
        //将图标放到罗盘中心
        compassView.setOnTouchListener(new View.OnTouchListener() {
            private float lockCoordinateX;//获取摇杆锁定时的横坐标
            private float lockCoordinateY;//获取摇杆锁定时的纵坐标
            private boolean isLongPress = false;//是否是长按
            private long touchTime;//按下的时间

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                drawJoystick(event);
                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN): {
                        joystickView.setBackgroundResource(R.drawable.float_window_joystick);
                        touchTime = System.currentTimeMillis();
                        isLongPress = false;
                        return true;
                    }
                    case (MotionEvent.ACTION_MOVE): {
                        maximumOffset = Math.max(Math.abs(lockCoordinateX - event.getX()), Math.abs(lockCoordinateY - event.getY()));
                        if (System.currentTimeMillis() - touchTime > lockJoystickDuration && !isLongPress ) {
                            //如果按下时间大于设定值且当前状态未锁定
                            touchTime = System.currentTimeMillis();
                            if (maximumOffset < longPressMaximumOffset) {
                                //如果按压点偏移量小于设定值
                                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(100, 10));
                                }
                                joystickView.setBackgroundResource(R.drawable.float_window_joystick_locked);
                                isLongPress = true;
                            }
                            maximumOffset = 0;
                        }
                        lockCoordinateX = event.getX();
                        lockCoordinateY = event.getY();
                        return true;
                    }
                    case (MotionEvent.ACTION_UP): {
                        if (!isLongPress) {
                            resetJoystick();
                        }
                        return true;
                    }
                }
                view.performClick();
                return false;
            }

            /**
             * 重置摇杆
             */
            private void resetJoystick() {
                currentProportion = 0;
                joystickView.setBackgroundResource(R.drawable.float_window_joystick);
                joystickView.setX(compassRadius - joystickRadius);
                joystickView.setY(compassRadius - joystickRadius);
            }

            /**
             * 绘制摇杆
             * @param event onTouchEvent
             */
            private void drawJoystick(MotionEvent event) {
                float eventX = event.getX();
                float eventY = event.getY();
                float centersDistance = (float) Math.sqrt(
                        Math.abs(
                                (eventX - compassRadius) * (eventX - compassRadius)
                                        + (eventY - compassRadius) * (eventY - compassRadius)));
                //两圆(指南针和摇杆)的中心距离
                if (centersDistance < compassRadius - joystickRadius) {
                    //如果摇杆在轮盘内
                    currentProportion = centersDistance / (compassRadius - joystickRadius);
                    joystickView.setY(eventY - joystickRadius);
                    joystickView.setX(eventX - joystickRadius);
                } else {
                    currentProportion = 1;
                    joystickView.setX(((compassRadius - joystickRadius) * (eventX - compassRadius) / centersDistance) + compassRadius - joystickRadius);
                    joystickView.setY(((compassRadius - joystickRadius) * (eventY - compassRadius) / centersDistance) + compassRadius - joystickRadius);
                }
                if (((eventX - compassRadius) / centersDistance) < 0) {
                    //如果COS值小于0,则在轮盘左侧
                    currentJoystickDirection = (float) Math.toDegrees(Math.atan((eventY - compassRadius) / (eventX - compassRadius))) + 180;//左
                } else {
                    currentJoystickDirection = (float) Math.toDegrees(Math.atan((eventY - compassRadius) / (eventX - compassRadius)));//右
                }
                if (!isCompassAvailable) {//当设备物理方向不可用
                    //更新方向
                    currentCompassDirection = currentJoystickDirection + lastDirection - 270;
                }
            }
        });


        //处理菜单键的拖动以及点击事件
        View menuView = floatingView.findViewById(R.id.float_window_menu);
        menuView.setOnTouchListener(new View.OnTouchListener() {
            private int coordinateX;
            private int coordinateY;
            private float initialTouchX;
            private float initialTouchY;
            private long compassTouchDownTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN): {
                        coordinateX = params.x;
                        coordinateY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        compassTouchDownTime = System.currentTimeMillis();
                        return true;
                    }
                    case (MotionEvent.ACTION_UP): {
                        if (((System.currentTimeMillis() - compassTouchDownTime) < 500)
                                && (Math.abs(event.getRawX() - initialTouchX) < 10)
                                && (Math.abs(event.getRawY() - initialTouchY) < 10)) {
                            //满足则为点击事件,打开菜单
                            if (expandedView.getVisibility() == View.INVISIBLE) {
                                expandedView.setVisibility(View.VISIBLE);
                            } else {
                                expandedView.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                    case (MotionEvent.ACTION_MOVE): {
                        params.x = coordinateX + (int) (event.getRawX() - initialTouchX);
                        params.y = coordinateY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    }
                }
                view.performClick();
                return false;
            }
        });
    }

    private void setupNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelID = getPackageName();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            channelID = "";
            NotificationChannel notificationChannel = new NotificationChannel(channelID, "MockService", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(notificationChannel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Floating Demo Running")
                .build();
        startForeground(1, notification);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }

}
