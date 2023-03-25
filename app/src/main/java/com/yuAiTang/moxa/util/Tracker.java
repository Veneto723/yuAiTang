package com.yuAiTang.moxa.util;

import android.content.Context;
import android.widget.Toast;
import com.yuAiTang.moxa.entity.Links;
import com.yuAiTang.moxa.entity.Tracks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Tracker {
    static StringBuilder tracks = new StringBuilder();
    private static final SimpleDateFormat format = (SimpleDateFormat) DateFormat.getDateTimeInstance();

    /**
     * 将积攒的日志文本，打印成文件保存，并清空tracks
     */
    public static void slice(Context context) {
        String path = "/sdcard";
        String mac = "08:AA:BB:CC:DD:01"; // TODO Utils.getMac()
        File directory = new File(path + "/logs");
        if (!directory.exists()) directory.mkdirs();

        SimpleDateFormat tempFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = tempFormat.format(new Date()) + "@" + mac;
        File file = new File(String.format("%s/logs/%s.txt", path, filename));
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(tracks.toString());
            writer.flush();
            writer.close();
            tracks.delete(0, tracks.length());
            tracks = new StringBuilder();
            new IConnector(Links.LOG, "", file).hyperReq(context, IConnector.Method.UPLOAD);
            Toast.makeText(context, "日志输出完毕", Toast.LENGTH_SHORT).show();
        } catch (IOException ignored) {
        }

        int now = Integer.parseInt(tempFormat.format(new Date()));
        File[] files = directory.listFiles();
        assert files != null;
        if (files.length > 0) {
            for (File target : files) {
                String name = target.getName();
                int time = Integer.parseInt(name.split("@")[0]);
                if (now - time > 24 * 60 * 60) target.deleteOnExit();
            }
        }
    }

    /**
     * 记录用户行为轨迹
     *
     * @param type 用户行为类型，具体类型见泛型Track
     * @see Tracks
     */
    public static void track(Context context, Tracks type, String msg) {
        tracks.append(String.format("[%s @ %s] %s\n", type.toString().toUpperCase(), format.format(new Date()), msg));
        if (type.equals(Tracks.reboot)) slice(context);
        if (tracks.length() >= 10000) slice(context);
    }

}
