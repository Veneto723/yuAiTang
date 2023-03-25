package com.yuAiTang.moxa.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import android_serialport_api.SerialPort;
import com.yuAiTang.moxa.entity.Tracks;
import com.yuAiTang.moxa.util.CommandPool;
import com.yuAiTang.moxa.util.ThreadPool;
import com.yuAiTang.moxa.util.Tracker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class SerialPortUnit {
    private final String tag;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private final String sendAction, receiveAction;
    private CommandPool pool;
    private boolean isSending;
    private final Context context;
    private int countdown;
    private boolean flag = false;

    /**
     * 构造方法，因为（目前）考虑到这几个串口都不会关闭，所以直接在constructor里开启
     *
     * @param service       用来维持这几个串口的Service类
     * @param port          串口地址
     * @param baud          串口波特率
     * @param filter        SerialPortUnit用于接收其他类传来指令时的BroadcastReceiver用IntentFilter
     * @param tag           串口单元Tag/Name用于辨识
     * @param sendAction    SerialPortUnit在广播自己给下位机下发指令时的Broadcast的Intent action
     * @param receiveAction SerialPortUnit在广播自己接到下位机上传指令时的Broadcast的Intent action
     * @param pool          CommandPool对象，用于存储、处理串口指令
     * @see SerialPortService
     * @see com.yuAiTang.moxa.util.CommandPool
     */
    public SerialPortUnit(Service service, String port, int baud, IntentFilter filter, String tag,
                          String sendAction, String receiveAction, CommandPool pool) {
        this.flag = false;
        this.tag = tag;
        this.sendAction = sendAction;
        this.receiveAction = receiveAction;
        this.context = service.getApplicationContext();
        // 设置广播
        ServiceReceiver receiver = new ServiceReceiver();
        service.registerReceiver(receiver, filter);
        // 开启串口&线程池&线程池发送Handler
        try {
            SerialPort mSerialPort = new SerialPort(new File(port), baud, 0);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
            ReadThread mReadThread = new ReadThread();
            mReadThread.start(); // 连接串口
            // 开启命令池CommandPool
            this.pool = pool;
            Log.i("Serial Port", tag + ": 串口开启成功");
            Tracker.track(context, Tracks.serialPort, tag + ": 串口开启成功");
        } catch (IOException e) {
            Log.i("Serial Port", tag + ": 串口地址有误，无法开启串口");
            Tracker.track(context, Tracks.serialPort, tag + ": 串口地址有误，无法开启串口");
            e.printStackTrace();
        }
    }


    // 读取串口回复线程
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    if (mInputStream == null) return;
                    byte[] buffer = new byte[mInputStream.available()];
                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        // 收到回复
                        onDataReceive(buffer, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /**
     * 接受串口回复
     *
     * @param buffer 回复输入流
     * @param size   回复字节个数/长度
     */
    protected void onDataReceive(final byte[] buffer, final int size) {
        countdown = 0;
        isSending = false;
        new Thread(() -> {
            String[] receive = new String[size];
            for (int i = 0; i < size; i++) {
                String temp = Integer.toHexString(buffer[i]).toUpperCase();
                if (temp.length() == 1) {
                    receive[i] = "0" + temp;
                } else if (temp.length() == 8) {
                    receive[i] = temp.substring(6, 8);
                } else {
                    receive[i] = temp;
                }
            }
            // 发送广播，表示以从单片机获取回复
            Log.i("收到信息<==", Arrays.toString(receive));
            Tracker.track(context, Tracks.serialPort, tag + "收到信息<==" + Arrays.toString(receive));
            Intent intent = new Intent(receiveAction);
            intent.putExtra("msg", Arrays.toString(receive));
            intent.putExtra("tag", tag);
            context.sendBroadcast(intent);
            // 对指令池进行操作
            try {
                ThreadPool.singleBlockedThread(() -> pool.dispel(receive));
                sendNext();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    Thread wait4Response = new Thread() {
        @Override
        public void run() {
            super.run();
            while(true){
                if(countdown > 1){
                    countdown = 0;
                    isSending = false;
                    sendNext();
                }
                countdown ++;
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private void sendNext() {
        ThreadPool.schedule(() -> {
        if (pool.size() > 0 && !isSending)
            sendData(pool.getFirst());
        }, 2000);
    }


    /**
     * 发出串口指令
     *
     * @param command 16进制指令
     */
    private void sendData(String[] command) {
        isSending = true;
        if (mOutputStream != null) {
            byte[] bytes = new byte[command.length];
            for (int i = 0; i < command.length; i++) {
                bytes[i] = (byte) Integer.parseInt(command[i], 16);
            }
            try {
                mOutputStream.write(bytes); // 十进制字节
                // 发送广播，表示以向串口下发该指令
                Log.i("发送信息==>", Arrays.toString(command));
                Tracker.track(context, Tracks.serialPort, tag + "发送信息==>" + Arrays.toString(command));
                Intent intent = new Intent(sendAction);
                intent.putExtra("msg", Arrays.toString(command));
                intent.putExtra("tag", tag);
                context.sendBroadcast(intent);
                switch (tag) {
                    case "S1":
                        pool.setLastCommandType(command[2]);
                        break;
                    case "S3":
                        pool.setLastCommandType(command[1]);
                        break;
                    case "S4":
                        pool.setLastCommandType("s4-unique");
                        break;
                }
                if(!flag){
                    flag = true;
//                    wait4Response.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("clearPool") != null) {
                pool.clear();
                isSending = false;
                Toast.makeText(context, "清空完毕", Toast.LENGTH_SHORT).show();
            } else {
                String[] command = intent.getStringArrayExtra("command");
                ArrayList<String> commands = intent.getStringArrayListExtra("commands");
                if (command == null && commands == null) {
                    Toast.makeText(context, "service接受参数缺失", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (command != null)  // 单个指令
                    pool.appendCommand(command);
                else  // 多指令
                    for (String raw : commands) pool.appendCommand(raw.split(" "));
                if (!isSending) sendData(pool.getFirst());
            }
        }
    }
}
