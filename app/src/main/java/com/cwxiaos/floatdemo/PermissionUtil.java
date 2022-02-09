package com.cwxiaos.floatdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {
    public boolean checkPermission(Context context){
        if(isPermissionGranted(context)){
            return true;
        }else {
            int requestCode = 1;
            String[] permissionString = {Manifest.permission.SYSTEM_ALERT_WINDOW};
            String alertDialogMessage = "需要浮窗权限";
            String alertDialogNegativeButtonTitle = "授权";
            boolean status = ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.SYSTEM_ALERT_WINDOW);
            if(!status) {
                //如果权限被拒且不再提示
                alertDialogNegativeButtonTitle = "设置";
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("需要权限");
            builder.setMessage(alertDialogMessage);
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton(alertDialogNegativeButtonTitle, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!status) {
                        if (openFloatWindowSettings(context)) {
                            Log.i(getClass() + "//checkPermission()", "Go to Settings");
                        }
                    }else {
                        ActivityCompat.requestPermissions((Activity) context, permissionString, requestCode);
                    }
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
        return false;
    }

    public boolean isPermissionGranted(Context context){
        return Settings.canDrawOverlays(context);
        //是否有浮窗权限
    }

    /**
     * 打开悬浮窗设置
     *
     * @param context ()
     * @return boolean 是否成功打开悬浮窗设置
     */
    private boolean openFloatWindowSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception exception) {
            Log.e(getClass() + "//openFloatWindowSettings()", exception.toString());
            exception.printStackTrace();
            return false;
        }
        return true;
    }

}
