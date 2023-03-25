package com.yuAiTang.moxa.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import androidx.annotation.Nullable;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.activity.fragment.Notification;
import com.yuAiTang.moxa.entity.Equipment;
import com.yuAiTang.moxa.entity.Links;
import com.yuAiTang.moxa.util.CommandPool;
import com.yuAiTang.moxa.util.IConnector;
import com.yuAiTang.moxa.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SerialPortService extends Service {

    private static final String[] PORTS = {"/dev/ttyS1", "/dev/ttyS3", "/dev/ttyS4"};
    private static final int[] BAUDS = {9600, 115200};

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SQLiteDatabase db = Connector.getDb(this);
        // S1串口：红外测温模块
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.yuAiTang.moxa.SerialPortUnit.s1");
        new SerialPortUnit(this, PORTS[0], BAUDS[1], filter, "S1",
                "com.yuAiTang.moxa.SerialPortUnit.s1.send",
                "com.yuAiTang.moxa.SerialPortUnit.s1.receive",
                new CommandPool(this.getApplicationContext(), null) {
                    @Override
                    public void dispel(String[] receive) {
                        this.shift();
                    }
                });

        // S3串口，64路单片机，控制艾灸设备状态指示
        filter = new IntentFilter();
        filter.addAction("com.yuAiTang.moxa.SerialPortUnit.s3");
        Context ctx = getApplicationContext();
        new SerialPortUnit(this, PORTS[1], BAUDS[1], filter, "S3",
                "com.yuAiTang.moxa.SerialPortUnit.s3.send",
                "com.yuAiTang.moxa.SerialPortUnit.s3.receive",
                new CommandPool(ctx, new String[]{"60"}) {
                    @Override
                    public void dispel(String[] receive) {
                        if (receive.length >= 3) {
                            String commandType = receive[1];
                            if (commandType.equals("60") && receive[2].equals("01")) {
                                prepend(new String[]{"AA", "60", "01", "01", "55"});
                            } else if (commandType.equals("60") && receive[2].equals("02")) {
                                if (getLastCommandType().equals(commandType)) shift();
                            } else if (commandType.equals("DD")) {
                                int id = Integer.parseInt(receive[2], 16);
                                Equipment.call(ctx, id);
                            } else if (commandType.equals("76")) {
                                shift();
                                // 握手反馈
                                Connector.updateLastRecord(db, SimpleDateFormat.getDateTimeInstance().format(new Date()), "/dev/ttyS3");
                                double temp = (Integer.parseInt(receive[3], 16) * 256 + Integer.parseInt(receive[2], 16)) / 100.;
                                SQLiteDatabase db = Connector.getDb(ctx);
                                Connector.insertTemp(db, temp, SimpleDateFormat.getDateInstance().format(new Date()));
                                Notification.notifyHandler handler = Notification.getHandler();
                                if(handler != null){
                                    Message msg = handler.obtainMessage();
                                    Bundle args = new Bundle();
                                    args.putString("type", "temperature");
                                    msg.setData(args);
                                    handler.sendMessage(msg);
                                }
                            } else if (commandType.equals("9C") && receive[2].equals("40")) {

                                try {
                                    JSONObject req = new JSONObject();
                                    req.put("mac", Utils.getMac());
                                    req.put("type", 3);
                                    new IConnector(Links.SUBMIT_OFF, req.toString()).hyperReq(ctx, IConnector.Method.POST);
                                    shift();
                                } catch (JSONException | SocketException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                shift();
                            }

                        }
                    }
                });
        // S4串口，16路继电器，控制艾灸设备供电
        filter = new IntentFilter();
        filter.addAction("com.yuAiTang.moxa.SerialPortUnit.s4");
        new SerialPortUnit(this, PORTS[2], BAUDS[0], filter, "S4",
                "com.yuAiTang.moxa.SerialPortUnit.s4.send",
                "com.yuAiTang.moxa.SerialPortUnit.s4.receive",
                new CommandPool(this.getApplicationContext(), null) {
                    @Override
                    public void dispel(String[] receive) {
                        if (receive.length == 15) {
                            if (receive[3].equals("67")) {
                                Connector.updateLastRecord(db, SimpleDateFormat.getDateTimeInstance().format(new Date()), "/dev/ttyS4");
                            }
                        }
                        this.shift();
                    }
                });
    }

}
