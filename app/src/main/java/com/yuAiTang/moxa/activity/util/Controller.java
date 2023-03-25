package com.yuAiTang.moxa.activity.util;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.yuAiTang.moxa.activity.template.EnhancedActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Controller {

    private static final ArrayList<EnhancedActivity> instances = new ArrayList<>();
    private static final HashMap<String, Bitmap> bitmaps = new HashMap<>();
    private static final String TAG = "CONTROLLER";

    public static void addActivity(EnhancedActivity activity){
        String name = activity.getClass().getName();
        if(exist(name)) removeActivity(name);
        instances.add(activity);
        Log.d(TAG, name + " is added");
    }

    public static void removeActivity(EnhancedActivity activity){
        instances.remove(activity);
        finishActivity(activity);
    }

    public static void removeActivity(String name){
        for(EnhancedActivity instance : instances){
            if(instance.getClass().getName().equals(name)){
                finishActivity(instance);
                instances.remove(instance);
            }
        }
    }

    private static void finishActivity(EnhancedActivity activity){
        if(!activity.isFinishing()) {
            // 关闭附属fragment(s)
            List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            for(Fragment fragment : fragments){
                transaction.remove(fragment);
            }
            transaction.commitAllowingStateLoss();
            activity.finish();
        }
    }

    public static Bitmap addBitmap(Bitmap bitmap, String name){
        if(notExistBitmap(name)) {
            bitmaps.put(name, bitmap);
        }
        return bitmaps.get(name);
    }

    public static void removeBitmap(String name){
        bitmaps.get(name).recycle();
        bitmaps.remove(name);
    }

    public static Bitmap getBitmap(String name){
        return bitmaps.get(name);
    }

    private static boolean exist(String name){
        for(EnhancedActivity instance : instances){
            String instanceName = instance.getClass().getName();
            if(name.equals(instanceName)){
                return true;
            }
        }
        return false;
    }

    public static boolean notExistBitmap(String name){
        return !bitmaps.containsKey(name);
    }

}
