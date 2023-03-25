package com.yuAiTang.moxa.Db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.yuAiTang.moxa.entity.Resource;

import java.util.LinkedHashMap;

public class DbInvoker extends SQLiteOpenHelper {

    public DbInvoker(Context context) {
        super(context, Constant.DB_NAME, null, Constant.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LinkedHashMap<String, String> equipment = new LinkedHashMap<>();
        equipment.put(Constant.ID, Constant.NUMERIC + " primary key not null");
        equipment.put(Constant.STATUS, Constant.NUMERIC);
        equipment.put(Constant.TYPE, Constant.NUMERIC);
        equipment.put(Constant.RELAY, Constant.NUMERIC + " unique");
        String sql = generator(Constant.TABLE_NAME_EQUIPMENT, equipment);
        db.execSQL(sql);
        LinkedHashMap<String, String> resource = new LinkedHashMap<>();
        resource.put(Constant.ID, Constant.NUMERIC + " primary key not null");
        resource.put(Constant.FILENAME, Constant.TEXT + " not null");
        resource.put(Constant.PATH, Constant.TEXT + " not null");
        resource.put(Constant.TYPE, Constant.NUMERIC);
        sql = generator(Constant.TABLE_NAME_RESOURCE, resource);
        db.execSQL(sql);
        LinkedHashMap<String, String> status = new LinkedHashMap<>();
        status.put(Constant.ID, "integer primary key not null");
        status.put(Constant.TEMPERATURE, Constant.REAL + " not null");
        status.put(Constant.TIME, Constant.DATETIME + " not null");
        sql = generator(Constant.TABLE_NAME_STATUS, status);
        db.execSQL(sql);
        LinkedHashMap<String, String> serialPort = new LinkedHashMap<>();
        serialPort.put(Constant.PORT, Constant.TEXT + "primary key not null");
        serialPort.put(Constant.BAUD, Constant.NUMERIC + " not null");
        serialPort.put(Constant.LAST_RECORD, Constant.DATETIME);
        sql = generator(Constant.TABLE_NAME_SERIAL_PORT, serialPort);
        db.execSQL(sql);

        // TODO temporary
        Resource res = new Resource(1, "1", "/sdcard/videos/1.mp4", 0);
        Connector.insertResource(db, res);
        res = new Resource(2, "2", "/sdcard/videos/2.mp4", 0);
        Connector.insertResource(db, res);
        res = new Resource(3, "3", "/sdcard/videos/3.mp4", 0);
        Connector.insertResource(db, res);
        res = new Resource(4, "4", "/sdcard/videos/4.mp4", 0);
        Connector.insertResource(db, res);
        res = new Resource(5, "5", "/sdcard/videos/5.mp4", 0);
        Connector.insertResource(db, res);
        res = new Resource(6, "6", "/sdcard/videos/6.mp4", 0);
        Connector.insertResource(db, res);
        Connector.insertPort(db, "/dev/ttyS1", 115200);
        Connector.insertPort(db, "/dev/ttyS3", 115200);
        Connector.insertPort(db, "/dev/ttyS4", 9600);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }

    /**
     * @param tableName 表名
     * @param tableMap  表内的所有变量集合
     * @return 创建表的sql语句
     */
    public String generator(String tableName, LinkedHashMap<String, String> tableMap) {
        StringBuilder sql = new StringBuilder("create table if not exists " + tableName + "(");
        for (String variable : tableMap.keySet()) {
            String suffix = tableMap.get(variable);
            sql.append(variable).append(" ").append(suffix).append(", ");
        }
        sql = new StringBuilder(sql.substring(0, sql.length() - 2)).append(");");
        return sql.toString();
    }

}
