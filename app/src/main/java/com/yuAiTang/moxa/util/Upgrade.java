package com.yuAiTang.moxa.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import com.yuAiTang.moxa.entity.Links;
import com.yuAiTang.moxa.entity.Tracks;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.util.TreeMap;

public class Upgrade {

    private static long download_id = -1;
    private static String apk_path;

    public static String need2Upgrade(Context ctx) {
        try {
            apk_path = "/sdcard/apks/" + Utils.nonce_str() + ".apk";
            TreeMap<String, String> obj = new TreeMap<>();
            obj.put("version", Utils.getAppVersion(ctx));
            IConnector connector = new IConnector(Links.APK + IConnector.args2Str(obj), "");
            Tracker.track(ctx, Tracks.upgrade, "开始APP版本升级");
            String resp = (String) connector.hyperReq(ctx, IConnector.Method.POST);
            JSONObject json = new JSONObject(resp);
            int code = json.getInt("code");
            if(code == 1)
                return json.getJSONObject("data").getString("location");
            else
                return "";
        } catch (JSONException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void upgrade(Context ctx){
        String location = need2Upgrade(ctx);
        if (!location.equals("")) {
            sendBroadcast(ctx, "downloading");
            download_id = DownloadUtil.download(ctx, Links.APK + "?location=" + location,
                    new File(apk_path), "");
        } else {
            sendBroadcast(ctx, "noNeed2Upgrade");
        }

    }

    public static int install(Context context, String apk_path) {
        String command = "pm install -r " + apk_path;
        Process process;
        DataOutputStream os;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            int exitValue = process.exitValue();
            Toast.makeText(context, "APK 安装结束：" + exitValue, Toast.LENGTH_LONG).show();
            os.close();
            process.destroy();
            return exitValue;
        } catch (Exception e) {
            Tracker.track(context, Tracks.upgrade, "APP版本升级失败\n" + e.toString());
            return 2;
        }
    }


    public static class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long ID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (download_id == ID) {
                Toast.makeText(ctx, "APK 下载完成!", Toast.LENGTH_LONG).show();
                sendBroadcast(ctx, "start_install");

                int result = install(ctx, apk_path);
                if (result == 0) {
                    Toast.makeText(ctx, "APK 升级成功!", Toast.LENGTH_LONG).show();
                    Tracker.track(ctx, Tracks.upgrade, "APP版本升级成功");
                    sendBroadcast(ctx, "install_success");
                } else {
                    Tracker.track(ctx, Tracks.upgrade, "APP版本升级失败, result=" + result);
                    Toast.makeText(ctx, "APK 升级失败!", Toast.LENGTH_LONG).show();
                    sendBroadcast(ctx, "install_fail");
                }
            }
        }
    }

    private static void sendBroadcast(Context context, String msg) {
        Intent intent = new Intent();
        intent.setAction("com.yuAiTang.moxa.upgrade");
        intent.putExtra("msg", msg);
        context.sendBroadcast(intent);
    }
}
