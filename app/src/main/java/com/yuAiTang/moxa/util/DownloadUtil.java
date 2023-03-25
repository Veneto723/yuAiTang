package com.yuAiTang.moxa.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedHashMap;

public class DownloadUtil {

    // 下载文件队列
    private static final LinkedHashMap<Long, String> queue = new LinkedHashMap<>();

    /**
     * 下载文件
     *
     * @param context  上下文，无意义
     * @param url      下载url
     * @param saveTo   保存文件
     * @param filename 文件名，用于Receiver检测
     * @return id 下载队列ID
     */
    public static long download(Context context, String url, File saveTo, String filename) {
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
        req.setDestinationUri(Uri.fromFile(saveTo));
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long id = manager.enqueue(req);
        queue.put(id, filename);
        return id;
    }

    public static class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long ID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (queue.containsKey(ID)) {
                Toast.makeText(context, "任务:" + ID + " 下载完成!", Toast.LENGTH_LONG).show();
                queue.remove(ID);
            }
        }
    }


}
