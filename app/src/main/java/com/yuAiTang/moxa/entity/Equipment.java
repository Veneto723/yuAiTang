package com.yuAiTang.moxa.entity;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.util.Tracker;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Equipment implements Serializable {
    private Integer id;
    private Integer status; // 0 occupied(红), 1 appointed(黄), 2 available(绿) ; BUT 微信小程序端服务器 0为空闲中1为已预约2为等待开启3为使用中4为设备故障
    private Integer type; // 设备种类 0 艾灸椅 1 艾灸舱 2 艾灸床; BUT 微信小程序端服务器 1为太空仓2为艾灸椅 3为艾灸床
    private Integer relay; // 继电器


    public Equipment(Integer id, Integer status, Integer type, int relay) {
        this.id = id;
        this.status = status;
        this.type = type;
        this.relay = relay;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getRelay() {
        return relay;
    }

    public void setRelay(Integer relay) {
        this.relay = relay;
    }

    public String[] stringify() {
        return new String[]{String.valueOf(id), String.valueOf(status), String.valueOf(type), String.valueOf(relay)};
    }

    public static Equipment getEquipFromJSON(int id, JSONObject json) throws JSONException {
        int serverType = json.getInt("type");
        int serverStatus = json.getInt("status");
        int type;
        if (serverType == 2) {
            type = 0;
        } else if (serverType == 3) {
            type = 2;
        } else {
            type = 1;
        }
        int status = 2 - serverStatus;
        return new Equipment(id, status, type, id);
    }

    /**
     * 给S3发送改变艾灸设备状态的命令组。因，命令重复性高，故写成方法
     *
     * @param ctx          上下文，无意义，用于发送广播
     * @param equip_id     设备ID
     * @param serverStatus 服务器端存储status ID
     */
    public static void changeStatus(SQLiteDatabase db, Context ctx, int equip_id, int serverStatus) {
        Tracker.track(ctx, Tracks.debug, "开始发送232指令");
        int localStatus = serverStatus;
        if (serverStatus == 2)
            localStatus = 0;
        else if (serverStatus == 0)
            localStatus = 2;
        Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s3");
        ArrayList<String> commands = new ArrayList<>();
        // 使用 预约 空闲
        int loopTimes = 0;
        int current = Connector.getEquipStatus(db, equip_id);
        if(current < localStatus){
            loopTimes = localStatus - current;
        }else if(current > localStatus){
            loopTimes = (3 - current) + localStatus;
        }
        Tracker.track(ctx, Tracks.debug, "指令将执行" + loopTimes + "次");
        String raw;
        for(int i = 0; i < loopTimes; i++) {
            raw = Integer.toHexString((equip_id - 1) * 8 + 1).toUpperCase(Locale.ROOT);
            raw = raw.length() < 2 ? "0" + raw : raw;
            commands.add("AA 97 00 03 55");
            commands.add("AA 98 " + raw + " 00 55");
        }
        if(commands.size() > 0) {
            intent.putExtra("commands", commands);
            ctx.sendBroadcast(intent);
            Tracker.track(ctx, Tracks.broadcast, Arrays.toString(commands.toArray(new String[]{})));
        }
    }

    /**
     * @param ctx      上下文，无意义，用于发送广播
     * @param equip_id 设备ID
     * @param startup  是否开启，true执行上电开机; false执行断点关机
     */
    public static void startup(Context ctx, int equip_id, boolean startup) {
        Tracker.track(ctx, Tracks.debug, "开始发送485指令");
        String keyHex;
        if (equip_id % 2 == 0) {
            if (startup) keyHex = "12";
            else keyHex = "02";
        } else {
            if (startup) keyHex = "21";
            else keyHex = "20";
        }

        int pos = (equip_id - 1) / 2;
        String[] command = new String[]{"48", "3A", "01", "57", "", "", "", "", "", "", "", "", "", "45", "44"};
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            if (i >= 4 && i < 12) {
                if (i - 4 == pos) command[i] = keyHex;
                else command[i] = "22";
            }
            if (i == 12) {
                String hex = Integer.toHexString(sum).toUpperCase(Locale.ROOT);
                command[i] = hex.length() > 2 ? hex.substring(hex.length() - 2) : hex;
            }
            sum += Integer.parseInt(command[i], 16);
        }
        Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s4");
        intent.putExtra("command", command);
        Tracker.track(ctx, Tracks.broadcast, Arrays.toString(command));

        ctx.sendBroadcast(intent);
    }

    /**
     * 固定开门指令
     *
     * @param ctx 上下文，无意义，用于发送广播
     */
    public static void openDoor(Context ctx) {
        Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s3");
        ArrayList<String> commands = new ArrayList<>();
        commands.add("AA 97 00 03 55");
        commands.add("AA 98 40 00 55");
        intent.putExtra("commands", commands);
        Tracker.track(ctx, Tracks.broadcast, Arrays.toString(commands.toArray(new String[]{})));
        ctx.sendBroadcast(intent);
    }

    public static void call(Context ctx, int equip_id) {
        // 发送给Main
        Intent intent = new Intent("com.yuAiTang.moxa.call");
        intent.putExtra("id", equip_id);
        ctx.sendBroadcast(intent);
    }
}
