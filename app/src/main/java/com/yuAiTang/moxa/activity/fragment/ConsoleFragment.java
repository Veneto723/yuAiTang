package com.yuAiTang.moxa.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.entity.Commands;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class ConsoleFragment extends Fragment {
    private final StringBuilder log = new StringBuilder();
    private TextView logTextView;
    private Spinner command, param, secondParam;
    private Button enter;

    private String lastCommandType = "";
    private logHandler handler;


    public void setLastCommandType(String lastCommandType) {
        this.lastCommandType = lastCommandType;
    }

    // 外部调用追加日志，其中会通过handler发送日志信息
    public void appendLog(String log, String tag) {
        this.log.append(tag).append(": ").append(log).append("\n");
        if (log.contains("<==")) {
            if (lastCommandType.split(":")[0].equals("s1")) {
                if (lastCommandType.split(":")[1].equals("04")) {
                    String rawStr = log.substring(log.indexOf("[") + 1, log.indexOf("]"));
                    String[] command = rawStr.split(",");

                    double temp = (Integer.parseInt(command[2].trim(), 16) + Integer.parseInt(command[3].trim(), 16) * 256) / 100.;
                    int dist = Integer.parseInt(command[4].trim(), 16) + Integer.parseInt(command[5].trim(), 16) * 256;
                    this.log.append("温度：").append(temp).append("℃，距离").append(dist).append("mm").append("\n");
                }
            }
            lastCommandType = "";
        }
        Message msg = handler.obtainMessage();
        msg.obj = this.log.toString();
        handler.sendMessage(msg);
    }

    // 这个handler本质就是追加日志文本
    private static class logHandler extends Handler {
        WeakReference<ConsoleFragment> consoleFrag;

        public logHandler(ConsoleFragment frag, @NonNull Looper looper) {
            super(looper);
            consoleFrag = new WeakReference<>(frag);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            ConsoleFragment frag = consoleFrag.get();
            super.handleMessage(msg);
            frag.logTextView.setText((String) msg.obj);
        }
    }

    // 清空日志
    public void clearLog() {
        this.log.delete(0, this.log.length());
        logTextView.setText(this.log.toString());
    }

    // 获取ConsoleFrag用户选择的待发送命令
    public String[] getInputCommand() {
        return (command.getSelectedItem().toString() + " " +
                param.getSelectedItem().toString() + " " +
                secondParam.getSelectedItem().toString()).split(" ");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LayoutInflater layoutInflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View root = layoutInflater.inflate(R.layout.console, container, false);
        logTextView = root.findViewById(R.id.log);
        command = root.findViewById(R.id.command);
        param = root.findViewById(R.id.param);
        secondParam = root.findViewById(R.id.secondParam);
        setAdapters(Commands.S1_COMMAND, Commands.S1_PARAM, Commands.S1_PARAM_2); // 设置默认指令组
        // 清空日志
        root.findViewById(R.id.clear).setOnClickListener(v -> clearLog());
        // 发送指令
        enter = root.findViewById(R.id.enter);
        setEnterListener(v -> {
            // TODO 发送串口指令
            String[] input = getInputCommand();
            String[] command = appendVerifiedSum(input);
            Intent intent = new Intent("com.yuAiTang.moxi.console");
            intent.putExtra("command", command);
            Objects.requireNonNull(getContext()).sendBroadcast(intent);
            lastCommandType = command[2];
        });
        handler = new logHandler(this, Looper.getMainLooper());
        return root;
    }

    // 修改ConsoleFragment的可发送指令组
    public void setAdapters(String[] command, String[] param, String[] param_2) {
        this.command.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item, command));
        this.param.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item, param));
        this.secondParam.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item, param_2));
    }

    // 修改ConsoleFragment中#enter(发送按键)的运行逻辑
    public void setEnterListener(View.OnClickListener listener) {
        enter.setOnClickListener(listener);
    }

    public static ConsoleFragment newInstance() {
        return new ConsoleFragment();
    }


    public String[] appendVerifiedSum(String[] command) {
        String[] resp = new String[command.length + 1];
        int sum = 0;
        for (int i = 0; i < command.length; i++) {
            sum += Integer.parseInt(command[i], 16);
            resp[i] = command[i];
        }
        String rawHex = Integer.toHexString(sum);
        resp[resp.length - 1] = rawHex.length() > 1 ? rawHex.substring(rawHex.length() - 2).toUpperCase() : "0" + rawHex.toUpperCase();
        return resp;
    }
}
