package com.yuAiTang.moxa.Db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.yuAiTang.moxa.entity.Equipment;
import com.yuAiTang.moxa.entity.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class Connector {
    private static DbInvoker invoker;
    private static SQLiteDatabase db;

    public static SQLiteDatabase getDb(Context context){
        if(db == null) {
            if (invoker == null) invoker = new DbInvoker(context);
            db = invoker.getWritableDatabase();
        }
        return db;
    }

    // 设备配置
    public static String getPort(SQLiteDatabase db){
        return "";
    }

    public static int getBaud(SQLiteDatabase db){
        return -1;
    }


    // 终端数据
    public static LinkedList<Equipment> getEquips(SQLiteDatabase db){
        LinkedList<Equipment> equips = new LinkedList<>();
        String sql = "select * from equipment order by id;";
        Cursor cursor = db.rawQuery(sql, null);
        while(cursor.moveToNext()){
            equips.add(new Equipment(cursor.getInt(cursor.getColumnIndex(Constant.ID)),
                    cursor.getInt(cursor.getColumnIndex(Constant.STATUS)),
                    cursor.getInt(cursor.getColumnIndex(Constant.TYPE)),
                    cursor.getInt(cursor.getColumnIndex(Constant.RELAY))));
        }
        cursor.close();
        return equips;
    }

    public static void importEquips(SQLiteDatabase db, ArrayList<Equipment> equips){
        String sql = "select id from equipment where id = ? LIMIT 1;";
        for(Equipment equipment : equips){
            Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(equipment.getId())});
            if(cursor.moveToFirst()){
                updateEquip(db, equipment.getType(), equipment.getRelay(), equipment.getId());
                updateStatus(db, equipment.getStatus(), equipment.getId());
            }else{
                insertEquip(db, equipment);
            }
            cursor.close();
        }
    }

    public static void insertEquip(SQLiteDatabase db, Equipment equip){
        String sql = "insert into equipment values(?, ?, ?, ?);";
        db.execSQL(sql, equip.stringify());
    }

    public static void deleteEquip(SQLiteDatabase db, int id){
        String sql = "delete from equipment where id = ?;";
        db.execSQL(sql, new String[]{String.valueOf(id)});
        rearrangeEquipIds(db, id);
    }

    public static int getEquipStatus(SQLiteDatabase db, int id){
        String sql = "select status from equipment where id = ?;";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});
        if(cursor.moveToFirst()){
            int status = cursor.getInt(cursor.getColumnIndex("status"));
            cursor.close();
            return status;
        }
        cursor.close();
        return -1;
    }

    public static void updateEquip(SQLiteDatabase db, int type, int relay, int id){
        String sql = "update equipment set type = ?, relay = ? where id = ?;";
        db.execSQL(sql, new String[]{String.valueOf(type), String.valueOf(relay), String.valueOf(id)});
    }

    public static void updateStatus(SQLiteDatabase db, int status, int id){
        String sql = "update equipment set status = ? where id = ?;";
        db.execSQL(sql, new String[]{String.valueOf(status), String.valueOf(id)});
    }

    public static void rearrangeEquipIds(SQLiteDatabase db, int deletedId){
        String sql = "select * from equipment where id > ? order by id;";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(deletedId)});
        sql = "update equipment set id = ? where id = ?";
        while(cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndex(Constant.ID));
            db.execSQL(sql, new String[]{String.valueOf(id - 1), String.valueOf(id)});
        }
        cursor.close();
    }

    // 播放资源管理

    public static LinkedHashMap<Integer, Resource> getResources(SQLiteDatabase db){
        LinkedHashMap<Integer, Resource> resources = new LinkedHashMap<>();
        String sql = "select * from resource order by id;";
        Cursor cursor = db.rawQuery(sql, null);
        while(cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndex(Constant.ID));
            resources.put(id, new Resource(cursor.getInt(cursor.getColumnIndex(Constant.ID)),
                    cursor.getString(cursor.getColumnIndex(Constant.FILENAME)),
                    cursor.getString(cursor.getColumnIndex(Constant.PATH)),
                    cursor.getInt(cursor.getColumnIndex(Constant.TYPE))));
        }
        cursor.close();
        return resources;
    }

    public static void insertResources(SQLiteDatabase db, List<String> paths){
        for(String path : paths) insertResource(db, path);
    }

    public static void insertResource(SQLiteDatabase db, String path){
        int index = path.lastIndexOf(".");

        int type = 1;
        if(Resource.isVideo(path.substring(index + 1))){
            type = 0;
        }
        String sql = "select id from resource order by id DESC LIMIT 1;";
        Cursor cursor = db.rawQuery(sql, null);
        int id = 1;
        if(cursor.moveToFirst()){
            id = cursor.getInt(cursor.getColumnIndex(Constant.ID)) + 1;
        }
        cursor.close();
        insertResource(db, new Resource(id, path.substring(path.lastIndexOf("/") + 1, index), path, type));
    }

    public static void insertResource(SQLiteDatabase db, Resource resource){
        String sql = "insert into resource values(?, ?, ?, ?);";
        db.execSQL(sql, resource.stringify());
    }

    public static LinkedHashMap<Integer, Resource> deleteResource(SQLiteDatabase db, int id){
        String sql = "delete from resource where id = ?";
        db.execSQL(sql, new String[]{String.valueOf(id)});
        rearrangeResource(db, id);
        return getResources(db);
    }

    public static void rearrangeResource(SQLiteDatabase db, int deletedId){
        String sql = "select * from resource where id > ? order by id;";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(deletedId)});
        sql = "update resource set id = id - 1 where id = ?;";
        while(cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndex(Constant.ID));
            db.execSQL(sql, new String[]{String.valueOf(id)});
        }
        cursor.close();
    }

    public static void upwardResource(SQLiteDatabase db, int targetId){
        String targetIdStr = String.valueOf(targetId);
        String objectIdStr = String.valueOf(targetId - 1);
        String sql = "update resource set id = ? where id = ?;";
        db.execSQL(sql, new String[]{ "-1", targetIdStr});
        db.execSQL(sql, new String[]{targetIdStr, objectIdStr});
        db.execSQL(sql ,new String[]{objectIdStr, "-1"});
    }

    public static void downwardResource(SQLiteDatabase db, int targetId){
        String targetIdStr = String.valueOf(targetId);
        String objectIdStr = String.valueOf(targetId + 1);
        String sql = "update resource set id = ? where id = ?;";
        db.execSQL(sql, new String[]{ "-1", targetIdStr});
        db.execSQL(sql, new String[]{targetIdStr, objectIdStr});
        db.execSQL(sql ,new String[]{objectIdStr, "-1"});
    }

    public static LinkedList<String> getResourceFiles(SQLiteDatabase db){
        String sql = "select path from resource order by id;";
        Cursor cursor = db.rawQuery(sql, null);
        LinkedList<String> paths = new LinkedList<>();
        while(cursor.moveToNext()){
            paths.add(cursor.getString(cursor.getColumnIndex("path")));
        }
        cursor.close();
        return paths;
    }


    // 设备温度状态

    public static double getLatestTemperature(SQLiteDatabase db){
        String sql = "select temperature from status order by id DESC LIMIT 1;";
        Cursor cursor = db.rawQuery(sql, null);
        if(cursor.moveToFirst()){
            double temp = cursor.getDouble(cursor.getColumnIndex(Constant.TEMPERATURE));
            cursor.close();
            return temp;
        }
        cursor.close();
        return 0;
    }

    public static void insertTemp(SQLiteDatabase db, double temperature, String now){
        String sql = "insert into status(temperature, time) values(?, ?);";
        db.execSQL(sql, new String[]{String.valueOf(temperature), now});
    }

    // 串口

    public static void insertPort(SQLiteDatabase db, String port, int baud){
        String sql = "insert into serial_port(port, baud, last_record) values(?, ?, ?);";
        db.execSQL(sql, new String[]{port, String.valueOf(baud), ""});
    }

    public static void updateLastRecord(SQLiteDatabase db, String time, String port){
        String sql = "update serial_port set last_record = ? where port = ?;";
        db.execSQL(sql, new String[]{time, port});
    }
}
