package com.yuAiTang.moxa.activity.template;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.yuAiTang.moxa.activity.fragment.ConsoleFragment;

public class ConsoleActivity extends EnhancedActivity {
    private ConsoleFragment console;
    private ReqReceiver req;
    private RespReceiver resp;
    private boolean receiverFlag = false;

    public void setConsole(ConsoleFragment console) {
        this.console = console;
    }

    public void setReceiver(ReqReceiver req, RespReceiver resp){
        if(!receiverFlag){
            this.req = req;
            this.resp = resp;
            IntentFilter reqFilter = new IntentFilter();
            reqFilter.addAction("com.yuAiTang.moxa.SerialPortUnit.s1.send");
            reqFilter.addAction("com.yuAiTang.moxa.SerialPortUnit.s3.send");
            reqFilter.addAction("com.yuAiTang.moxa.SerialPortUnit.s4.send");
            IntentFilter respFilter = new IntentFilter();
            respFilter.addAction("com.yuAiTang.moxa.SerialPortUnit.s1.receive");
            respFilter.addAction("com.yuAiTang.moxa.SerialPortUnit.s3.receive");
            respFilter.addAction("com.yuAiTang.moxa.SerialPortUnit.s4.receive");
            registerReceiver(this.req, reqFilter);
            registerReceiver(this.resp, respFilter);
            this.receiverFlag = true;
        }
    }

    // 广播接收Service传回的串口反馈，并更新到consoleFragment
    public class ReqReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            console.appendLog("发出指令==>" + intent.getStringExtra("msg"), intent.getStringExtra("tag"));
        }
    }

    public class RespReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

            console.appendLog("收到指令<==" + intent.getStringExtra("msg"), intent.getStringExtra("tag"));
        }
    }

    @Override
    protected void onPause() {
        if (req != null && receiverFlag) {
            receiverFlag = false;
            unregisterReceiver(req);
            unregisterReceiver(resp);
        }
        super.onPause();
    }
}
