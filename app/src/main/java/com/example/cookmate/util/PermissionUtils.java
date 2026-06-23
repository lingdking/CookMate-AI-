package com.example.cookmate.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    // 请求码
    public static final int REQUEST_CAMERA = 100;
    public static final int REQUEST_LOCATION = 101;
    public static final int REQUEST_STORAGE = 102;

    /**
     * 检查和请求所有必要权限
     * @return 未被授权的权限列表
     */
    public static String[] checkAndRequestAllPermissions(Activity activity) {
        List<String> neededPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    neededPermissions.toArray(new String[0]),
                    999
            );
        }

        return neededPermissions.toArray(new String[0]);
    }

    /**
     * 检查相机权限
     */
    public static boolean hasCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求相机权限
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA
        );
    }

    /**
     * 检查位置权限
     */
    public static boolean hasLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求位置权限
     */
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION
        );
    }
}