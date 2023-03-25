package com.yuAiTang.moxa.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import cockroach.Cockroach;
import cockroach.ExceptionHandler;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.activity.fragment.*;
import com.yuAiTang.moxa.activity.template.EnhancedActivity;
import com.yuAiTang.moxa.activity.util.Viewer;
import com.yuAiTang.moxa.entity.Attribute;
import com.yuAiTang.moxa.entity.Equipment;
import com.yuAiTang.moxa.entity.Links;
import com.yuAiTang.moxa.entity.Tracks;
import com.yuAiTang.moxa.service.ProcessorService;
import com.yuAiTang.moxa.service.SerialPortService;
import com.yuAiTang.moxa.util.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Future;

public class Main extends EnhancedActivity implements FragmentListener {
    private final HashMap<Integer, EquipmentFragment> equipFrags = new HashMap<>(); // 键: 设备ID，值: EquipFrag
    private FaceFragment faceFragment; // 摄像头（预览\截图）Fragment
    private FaceFragment.FaceHandler faceHandler;
    private ResultFragment resultFragment; // 舌诊结果展示Fragment
    private Notification notification; // 顶部header
    private Viewer viewer; // 视频/图片播放管理器
    private UIHandler uiHandler;
    private Future<?> timedFuture; // 检测完毕倒计时返回视频/图片播放画面
    private int countdown = 60; // 检测完毕倒计时返回视频/图片播放画面
    private FrameLayout loading; // 等待加载蒙层
    private SQLiteDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        loading = findViewById(R.id.loading);
        startService(new Intent(this, SerialPortService.class));
        startService(new Intent(this, ProcessorService.class)); //频繁向服务器获取信息
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.yuAiTang.moxa.SerialPortUnit.s1.receive");
        filter.addAction("com.yuAiTang.moxa.serverService.showFace");
        filter.addAction("com.yuAiTang.moxa.serverService.showTongue");
        filter.addAction("com.yuAiTang.moxa.changeEquip");
        filter.addAction("com.yuAiTang.moxa.call");
        filter.addAction("com.yuAiTang.moxa.notify");
        filter.addAction("com.yuAiTang.moxa.internet");
        filter.addAction("com.yuAiTang.moxa.temperature");
        registerReceiver(receiver, filter);
        uiHandler = new UIHandler(this, Looper.myLooper());
        Sync.sync(this); // 与服务器同步状态
    }

    @Override
    protected void onStart() {
        super.onStart();
        db = Connector.getDb(this);
        String apk_location = Upgrade.need2Upgrade(this);
        if(!apk_location.equals("")){
            String raw = apk_location.substring(apk_location.lastIndexOf("/") + 1);
            try {
                String targetVersion = raw.substring(0, raw.length() - 4);
                String currentVersion = Utils.getAppVersion(this);
                TextView view = findViewById(R.id.upgrateHint);
                view.setText(String.format(getResources().getString(R.string.upgradeHint), currentVersion, targetVersion));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            findViewById(R.id.upgradeBanner).setVisibility(View.VISIBLE);
            findViewById(R.id.quit).setOnClickListener(v->{
                findViewById(R.id.upgradeBanner).setVisibility(View.GONE);
            });
            findViewById(R.id.upgrade).setOnClickListener(v->{
                findViewById(R.id.upgradeBtns).setVisibility(View.GONE);
                findViewById(R.id.upgradeProgress).setVisibility(View.VISIBLE);
                Upgrade.upgrade(this);
            });
        }else{
            findViewById(R.id.upgradeBanner).setVisibility(View.GONE);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        String notify = "{煜艾堂科技养生馆}上海-闵行-863园区店欢迎您！ WiFi：yuaitang  密码：88888888";
        notification = Notification.newInstance(notify);
        transaction.add(R.id.notification, notification, "notification");

        faceFragment = FaceFragment.newInstance();
        transaction.add(R.id.facial, faceFragment, "face");

        LinkedList<Equipment> equipments = Connector.getEquips(db);
        for (Equipment equipment : equipments) {
            EquipmentFragment frag = EquipmentFragment.newInstance(equipment);
            equipFrags.put(equipment.getId(), frag);
            transaction.add(R.id.equipments, frag, "equipment#" + equipment.getId());
        }
        transaction.commit();
        // 轮播图管理
        LinkedList<String> paths = Connector.getResourceFiles(db);
        viewer = new Viewer(paths, findViewById(R.id.imageView), findViewById(R.id.videoView));
        viewer.start();
        installCockroach();
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewer.stop();
    }

    @Override
    public void sendValue(String signal, Object value) {
        Bundle args;
        switch (signal) {
            case "enter":
                Tracker.track(this, Tracks.enter, "进入后台管理系统，时间： " + Utils.getCurrentTime());
                startActivity(new Intent(this, Manager.class));
                this.finish();
                break;
            case "face-recognition:ok":
                findViewById(R.id.facialBox).setVisibility(View.VISIBLE);
                sendCommand(new String[]{"A5", "55", "04", "FB"}); // 测温指令
                ((TextView) findViewById(R.id.hint)).setText("体温检测中 。。。");
                ((TextView) findViewById(R.id.upperAlert)).setText("体温检测中 。。。");
                ((TextView) findViewById(R.id.lowerAlert)).setText("体温检测中 。。。");
                break;
            case "bpm-recognition:ok":
                String status = (int) value >= 55 && (int) value <= 100 ? "正常" : "异常";
                ((TextView) findViewById(R.id.heartRate)).setText(String.format(getResources().getString(R.string.heart_rate), value, status));
                stopPreview();
                findViewById(R.id.after).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.hint)).setText("检测完毕");
                timedFuture();
                break;
            case "tongue-recognition:ok":
                /* 此处应该有以下一系列操作
                1. 将图片上传至服务器
                2. 服务器端，利用上传的图片以及其他数据，调用第三方接口。并返回过滤好的数值
                3. 本地，接受数值反馈并展示

                TODO 目前，因为服务器端还未搭建。所以只是展示默认数值
                 */
                try {
                    JSONObject json = new JSONObject();
                    json.put("tongue", value);
                    System.out.println(json.toString());
                    IConnector connector = new IConnector(Links.TONGUE, json.toString());
                    Object resp = connector.hyperReq(this, IConnector.Method.POST);
                    System.out.println(resp);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                stopPreview();
                findViewById(R.id.tongueResult).setVisibility(View.VISIBLE);
                args = new Bundle();
                args.putSerializable("attr", Attribute.genAttribute(0));
                args.putString("testTime", Utils.getCurrentTime());
                args.putString("testId", "A000123");
                args.putParcelable("qrcode", Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888));
                if (resultFragment == null) { // 没有就追加Fragment
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    resultFragment = ResultFragment.newInstance(args);
                    transaction.add(R.id.tongueResult, resultFragment, "result");
                    transaction.commit();
                } else { // 有就修改Fragment的内容
                    resultFragment.updateValues(args);
                }
                ((TextView) findViewById(R.id.hint)).setText("检测完毕");
                timedFuture();
                break;
            case "equipment#1":
                startFaceDetect();
                break;
            case "equipment#2":
                startTongueDetect();
                break;
        }
    }

    private void timedFuture() {
        timedFuture = ThreadPool.scheduleAtRate(() -> {
            Message msg = uiHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("msg", "detect:countdown");
            msg.setData(bundle);
            uiHandler.sendMessage(msg);
            if (countdown == 0) {
                msg = uiHandler.obtainMessage();
                bundle = new Bundle();
                bundle.putString("msg", "viewer:display");
                msg.setData(bundle);
                uiHandler.sendMessage(msg);
            }
        }, 0, 1000);
    }

    private static class UIHandler extends Handler {
        WeakReference<Main> main;

        public UIHandler(Main main, Looper looper) {
            super(looper);
            this.main = new WeakReference<>(main);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Main main = this.main.get();
            Bundle args = msg.getData();
            switch (msg.getData().getString("msg", "")) {
                case "viewer:display":
                    main.viewer.resume();
                    main.findViewById(R.id.videoView).setVisibility(View.VISIBLE);
                    main.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
                    main.findViewById(R.id.facial).setVisibility(View.GONE);
                    main.findViewById(R.id.tongueResult).setVisibility(View.GONE);
                    main.findViewById(R.id.facialBox).setVisibility(View.GONE);
                    break;
                case "detect:display":
                    main.findViewById(R.id.facial).setVisibility(View.VISIBLE);
                    main.findViewById(R.id.tongueResult).setVisibility(View.GONE);
                    main.findViewById(R.id.facialBox).setVisibility(View.GONE);
                    main.findViewById(R.id.videoView).setVisibility(View.GONE);
                    main.findViewById(R.id.imageView).setVisibility(View.GONE);
                    break;
                case "detect:countdown":
                    if (main.countdown == 0) {
                        main.timedFuture.cancel(true);
                        main.countdown = 60;
                    } else
                        ((TextView) main.findViewById(R.id.hint)).setText("检测完毕 (" + main.countdown-- + "s)");
                    break;
                case "changeEquip:status":
                    try {
                        Objects.requireNonNull(main.equipFrags.get(args.getInt("id"))).
                                changeStatus(args.getInt("status"));
                    } catch (NullPointerException ignored) {
                    }
                    break;
                case "changeEquip:call":
                    try {
                        Objects.requireNonNull(main.equipFrags.get(args.getInt("id"))).call();
                    } catch (NullPointerException ignored) {
                    }
                    break;
                case "notification":
                    if(main.notification != null) main.notification.setInfo(args.getString("notify"));
                    break;
                case "internet":
                    if(main.notification != null) main.notification.setInternet(args.getInt("internet"));
                    break;
            }
        }
    }

    public void stopPreview() {
        Message message = faceHandler.obtainMessage();
        Bundle args = new Bundle();
        args.putInt("type", 1);
        message.setData(args);
        faceHandler.sendMessage(message);
    }

    public void refreshFacialDetect() {
        loading.setVisibility(View.VISIBLE);
        findViewById(R.id.facial).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.bodyTemp)).setText("体温检测中 ...");
        ((TextView) findViewById(R.id.heartRate)).setText("心率检测中 ...");
        if (faceHandler == null) faceHandler = new FaceFragment.FaceHandler(faceFragment, Looper.myLooper());
        Message msg = faceHandler.obtainMessage();
        Bundle args = new Bundle();
        args.putInt("type", -1);
        msg.setData(args);
        faceHandler.sendMessage(msg);

        msg = uiHandler.obtainMessage();
        args = new Bundle();
        args.putString("msg", "detect:display");
        msg.setData(args);
        uiHandler.sendMessage(msg);
        viewer.pause();
        if (timedFuture != null) {
            timedFuture.cancel(true);
            countdown = 60;
        }
    }

    public void startFaceDetect() {
        refreshFacialDetect();

        // 修改UI展示，展示检测UI，隐藏ViewerUI
        // FaceFragment
        Message msg = faceHandler.obtainMessage();
        Bundle args = new Bundle();
        msg.obj = loading;
        args.putInt("type", 0);
        msg.setData(args);
        faceHandler.sendMessage(msg);
    }

    public void startTongueDetect() {
        refreshFacialDetect();

        // FaceFragment
        Message msg = faceHandler.obtainMessage();
        msg.obj = loading;
        Bundle args = new Bundle();
        args.putInt("type", 2);
        msg.setData(args);
        faceHandler.sendMessage(msg);
    }

    private void sendCommand(String[] command) {
        Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s1");
        intent.putExtra("command", command);
        intent.putExtra("port", "/dev/ttyS1");
        intent.putExtra("baud", 115200);
        sendBroadcast(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case "com.yuAiTang.moxa.serverService.showFace":
                    startFaceDetect();
                    break;
                case "com.yuAiTang.moxa.serverService.showTongue":
                    startTongueDetect();
                    break;
                case "com.yuAiTang.moxa.changeEquip": {
                    int id = intent.getIntExtra("id", -1);
                    int status = intent.getIntExtra("status", -1);
                    if (id != -1 && (status == 0 || status == 1 || status == 2)) {
                        Message msg = uiHandler.obtainMessage();
                        Bundle args = new Bundle();
                        args.putString("msg", "changeEquip:status");
                        args.putInt("id", id);
                        args.putInt("status", status);
                        msg.setData(args);
                        uiHandler.sendMessage(msg);
                    }
                    break;
                }
                case "com.yuAiTang.moxa.call": {
                    int id = intent.getIntExtra("id", -1);
                    if (id != -1) {
                        Message msg = uiHandler.obtainMessage();
                        Bundle args = new Bundle();
                        args.putString("msg", "changeEquip:call");
                        args.putInt("id", id);
                        msg.setData(args);
                        uiHandler.sendMessage(msg);
                    }
                    break;
                }
                case "com.yuAiTang.moxa.notify":
                    Message msg = uiHandler.obtainMessage();
                    Bundle args = new Bundle();
                    args.putString("msg", "notification");
                    args.putString("notify", intent.getStringExtra("notify"));
                    msg.setData(args);
                    uiHandler.sendMessage(msg);
                    break;
                case "com.yuAiTang.moxa.internet":
                    msg = uiHandler.obtainMessage();
                    args = new Bundle();
                    args.putString("msg", "internet");
                    args.putInt("internet", intent.getIntExtra("internet", -1));
                    msg.setData(args);
                    uiHandler.sendMessage(msg);
                    break;
                default:
                    String raw = intent.getStringExtra("msg");
                    String[] command = raw.substring(raw.indexOf("[") + 1, raw.indexOf("]")).split(",");
                    double temp = 0;
                    if(command.length >= 8)
                        temp = (Integer.parseInt(command[2].trim(), 16) + Integer.parseInt(command[3].trim(), 16) * 256) / 100.;
                    if (temp <= 10) {
                        sendCommand(new String[]{"A5", "55", "04", "FB"}); // 测温指令
                    } else {
                        String status = temp >= 35 && temp <= 39 ? "正常" : "异常";
                        ((TextView) findViewById(R.id.bodyTemp)).setText(String.format(getResources().getString(R.string.body_temp), temp, status));
                        ((TextView) findViewById(R.id.upperAlert)).setText("心率检测中，请保持不动5–10秒");
                        ((TextView) findViewById(R.id.lowerAlert)).setText("心率检测中，请保持不动5–10秒");
                    }
                    break;
            }
        }
    };


    private void installCockroach() {
        Context this_ = this;
        Cockroach.install(this, new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                Log.e("AndroidRuntime", "ExceptionHappened:" + thread, throwable);
                Tracker.track(this_, Tracks.debug, throwable.toString());
                new Handler(Looper.getMainLooper()).post(() -> {});
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                throwable.printStackTrace();//打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
            }

            @Override
            protected void onEnterSafeMode() {
                Toast.makeText(this_, "进入安全模式", Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onMayBeBlackScreen(Throwable throwable) {
                Thread thread = Looper.getMainLooper().getThread();
                Log.e("AndroidRuntime", "ExceptionHappened:" + thread, throwable);
                Tracker.track(this_, Tracks.debug, throwable.toString() + "\n" + throwable.getMessage() + "\n" + Objects.requireNonNull(throwable.getCause()).toString());
            }
        });
    }
}
