package com.yuAiTang.moxa.util;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.entity.Equipment;
import com.yuAiTang.moxa.entity.Links;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketException;
import java.util.ArrayList;

public class Sync {
    public static void sync(Context ctx) {
        try {
            String mac = Utils.getMac();
            JSONObject json = new JSONObject();
            mac = "08:AA:BB:CC:DD:01"; // TODO need to delete
            json.put("mac", mac);
            IConnector connector = new IConnector(Links.MOX_INFO, json.toString());
            Object rawResp = connector.hyperReq(ctx, IConnector.Method.POST);
            if (rawResp == null) {
                sendBroadcast(ctx, "network fail");
            } else {
                JSONObject respJSON = new JSONObject((String) rawResp);
                if(respJSON.getInt("code") != 1){
                    sendBroadcast(ctx, respJSON.getString("msg"));
                }else{
                    JSONArray equipJSON = respJSON.getJSONObject("data").getJSONArray("mox");
                    String notify = respJSON.getJSONObject("data").getString("remark");
                    Intent intent = new Intent("com.yuAiTang.moxa.notify");
                    intent.putExtra("notify", notify);
                    ctx.sendBroadcast(intent);
                    ArrayList<Equipment> equips = new ArrayList<>();
                    for(int i = 0; i < equipJSON.length(); i++)
                        equips.add(Equipment.getEquipFromJSON(i + 1, equipJSON.getJSONObject(i)));
                    SQLiteDatabase db = Connector.getDb(ctx);
                    Connector.importEquips(db, equips);
                    sendBroadcast(ctx, "init success");
                }
            }
        } catch (SocketException e) {
            // 获取Mac地址失败
            sendBroadcast(ctx, "fetch mac fail");
            e.printStackTrace();
        } catch (JSONException e) {
            // json异常
            sendBroadcast(ctx, "json fail");
            e.printStackTrace();
        }
    }

    private static void sendBroadcast(Context ctx, String msg) {
        Intent intent = new Intent("com.yuAiTang.moxi.initializer");
        intent.putExtra("initializer:msg", msg);
        ctx.sendBroadcast(intent);

    }
}
