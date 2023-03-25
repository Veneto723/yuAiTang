package com.yuAiTang.moxa.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.activity.template.EnhancedActivity;
import com.yuAiTang.moxa.entity.Tracks;
import com.yuAiTang.moxa.util.Tracker;
import com.yuAiTang.moxa.util.Upgrade;

import java.io.IOException;

public class Manager extends EnhancedActivity {

    private LinearLayout upgrade;
    private ManagerReceiver receiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manager);
        GridLayout root = findViewById(R.id.root);
        for (int childAt = 0; childAt < root.getChildCount(); childAt++) {
            root.getChildAt(childAt).setOnClickListener(listener);
        }
        upgrade = findViewById(R.id.upgrade);
        receiver = new ManagerReceiver();
        registerReceiver(receiver, new IntentFilter("com.yuAiTang.moxa.upgrade"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (receiver != null) unregisterReceiver(receiver);
    }

    private final View.OnClickListener listener = v -> {
        ViewGroup layout = (ViewGroup) v;
        v.setBackgroundResource(R.drawable.color_primary_background);
        ((TextView) layout.getChildAt(1)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        ImageView icon = (ImageView) layout.getChildAt(0);
        switch (v.getId()) {
            case R.id.adjust:
                icon.setImageResource(R.drawable.adjust_white);
                startActivity(new Intent(this, SerialPortTest.class));
                break;
            case R.id.restore:
                icon.setImageResource(R.drawable.restore_white);
                break;
            case R.id.exit:
                icon.setImageResource(R.drawable.exit_white);
                startActivity(new Intent(this, Main.class));
                this.finish();
                break;
            case R.id.equip_setting:
                icon.setImageResource(R.drawable.equip_setting_white);
                startActivity(new Intent(this, EquipmentSetting.class));
                break;
            case R.id.upgrade:
                icon.setImageResource(R.drawable.upgrade_white);
                try {
                    Upgrade.upgrade(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.reboot:
                icon.setImageResource(R.drawable.reboot_white);
                try {
                    Tracker.track(this, Tracks.reboot, "后台管理系统 => 重启");
                    Runtime.getRuntime().exec("/system/bin/reboot").waitFor();
                } catch (IOException | InterruptedException ignored) {
                }
                break;
            case R.id.upload:
                icon.setImageResource(R.drawable.upload_white);
                Tracker.slice(this);
                break;
            case R.id.resources:
                icon.setImageResource(R.drawable.resources_white);
                startActivity(new Intent(this, ResourcesManager.class));
                break;
        }
    };

    private final class ManagerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.yuAiTang.moxa.upgrade")) {
                TextView indicator = (TextView) upgrade.getChildAt(1);
                String msg = intent.getStringExtra("msg");
                switch (msg) {
                    case "noNeed2Upgrade":
                        indicator.setText("无需升级");
                        break;
                    case "downloading":
                        indicator.setText("下载中 ...");
                        break;
                    case "start_install":
                        indicator.setText("开始升级");
                        break;
                    case "install_success":
                        indicator.setText("升级成功");
                        break;
                    case "install_fail":
                        indicator.setText("升级失败");
                        break;
                    case "internet_fail":
                        indicator.setText("网络连接失败");
                        break;
                }
            }
        }
    }
}
