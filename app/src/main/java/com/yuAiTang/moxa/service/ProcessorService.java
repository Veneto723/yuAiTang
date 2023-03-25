package com.yuAiTang.moxa.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import androidx.annotation.Nullable;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.activity.fragment.Notification;
import com.yuAiTang.moxa.entity.Equipment;
import com.yuAiTang.moxa.entity.Links;
import com.yuAiTang.moxa.entity.Tracks;
import com.yuAiTang.moxa.util.IConnector;
import com.yuAiTang.moxa.util.Processor;
import com.yuAiTang.moxa.util.ThreadPool;
import com.yuAiTang.moxa.util.Tracker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProcessorService extends Service {

    private final ArrayList<Integer> appointingEquips = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        SQLiteDatabase db = Connector.getDb(this);

        // 2s interval
        Context ctx = this;
        String mac = "08:AA:BB:CC:DD:01"; //TODO Utils.getMac()
        JSONObject json = new JSONObject();
        try {
            json.put("type", 1);
            json.put("mac", mac);
        } catch (JSONException ignored) {
        }
        IConnector connector = new IConnector(Links.MOX_STATUS, json.toString());
        IConnector submit_off_connector = new IConnector(Links.SUBMIT_OFF, json.toString());

        final Notification.notifyHandler handler = Notification.getHandler();
        Processor processor = new Processor(0, 2 * 1000) {
            @Override
            public void run() throws Exception {
                // Notification时间
                if (handler != null) {
                    Bundle args = new Bundle();
                    args.putString("type", "timeUtil");
                    Message msg = handler.obtainMessage();
                    msg.setData(args);
                    handler.sendMessage(msg);
                }

                // Moxa status

                String rawResp = (String) connector.hyperReq(ctx, IConnector.Method.POST);
                JSONArray data = new JSONObject(rawResp).getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject resp = data.getJSONObject(i);
                    int id = resp.getInt("id"); // 待开启艾灸设备ID，发送给64路
                    int status = resp.getInt("status");
                    int localStatus = status;
                    if (status == 2) localStatus = 0;
                    else if (status == 0) localStatus = 2;
                    Intent intent = new Intent("com.yuAiTang.moxa.changeEquip");
                    intent.putExtra("id", id);
                    intent.putExtra("status", localStatus);
                    sendBroadcast(intent);
                    if (status == 2 || status == 1) {
                        int min = resp.getInt("min"); // 开启时间（min），创建定时器调用16路继电器
                        // 这里因为服务器无法主动发起对特定安卓端通讯。所以需要安卓端来定时查询服务器状态
                        // 所以这边要判断一下，艾灸设备是否已经在工作。因为服务器端只有 关机/开机两个状态

                        Tracker.track(ctx, Tracks.debug, "设备是否处于预约中？" + appointingEquips.contains(id));
                        if (status == 2 || !appointingEquips.contains(id)) {
                            Equipment.changeStatus(db, ctx, id, status);
                            Connector.updateStatus(db, localStatus, id);
                            if (status == 1) appointingEquips.add(id);
                            if (status == 2) {
                                Equipment.startup(ctx, id, true);
                                ThreadPool.schedule(() -> {
                                    try {
                                        Tracker.track(ctx, Tracks.debug, "倒计时结束，开始关闭设备并调整LED状态");
                                        JSONObject json = new JSONObject();
                                        json.put("mac", mac);
                                        json.put("type", 2);
                                        json.put("mid", id);
                                        new IConnector(Links.MOX_STATUS, json.toString()).hyperReq(ctx, IConnector.Method.POST);
                                        json = new JSONObject();
                                        json.put("mac", mac);
                                        json.put("type", 3);
                                        json.put("mid", id);
                                        new IConnector(Links.MOX_STATUS, json.toString()).hyperReq(ctx, IConnector.Method.POST);
                                        Equipment.changeStatus(db, ctx, id, 0);
                                        Equipment.startup(ctx, id, false);
                                        appointingEquips.remove(id);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }, (long) min * 60 * 1000);
                            }
                        }
                    }
                }

                // Door status
                Object resp = submit_off_connector.hyperReq(ctx, IConnector.Method.POST);
                JSONObject jsonObject = new JSONObject((String) resp);
                if (jsonObject.getInt("code") == 1) {
                    int needToOpen = jsonObject.getJSONObject("data").getInt("submit_open");
                    if (needToOpen == 2) {
                        Equipment.openDoor(ctx);
                        JSONObject req = new JSONObject();
                        req.put("mac", mac);
                        req.put("type", 2);
                        new IConnector(Links.SUBMIT_OFF, req.toString()).hyperReq(ctx, IConnector.Method.POST);
                    }
                }

                // Internet status TODO !temporary!
                Intent intent = new Intent("com.yuAiTang.moxa.internet");
                int status = 0; // 网络状态 0无网络， 1 差, 2 中, 3 优， 4 有线
                ConnectivityManager connManager = (ConnectivityManager) getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                boolean isCableEnabled = networkInfo.isConnected();
                if (isCableEnabled) { // 有线网络
                    status = 4;
                } else {
                    WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
                    int wifi = mWifiInfo.getRssi();//获取wifi信号强度
                    if (wifi > -50 && wifi < 0) {
                        status = 3;
                    } else if (wifi > -80 && wifi < -50) {
                        status = 2;
                    } else if (wifi > -100 && wifi < -80) {
                        status = 1;
                    }
                }
                intent.putExtra("internet", status);
                sendBroadcast(intent);
            }
        };
        processor.process(this);

        // 3 min interval

        Processor processor2 = new Processor(0, 3 * 60 * 1000) {
            @Override
            public void run() throws Exception {
                // 232握手
                Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s3");
                intent.putExtra("command", new String[]{"AA", "76", "01", "01", "55"});
                sendBroadcast(intent);
                // 485握手
                intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s4");
                intent.putExtra("command", new String[]{"48", "3A", "01", "66", "00", "00", "00", "00", "00", "00", "00", "00", "E9", "45", "44"});
                sendBroadcast(intent);
                // 温度更新
                if (handler != null) {
                    Bundle args = new Bundle();
                    args.putString("type", "temperature");
                    Message msg = handler.obtainMessage();
                    msg.setData(args);
                    handler.sendMessage(msg);
                }
            }
        };
        processor2.process(this);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
