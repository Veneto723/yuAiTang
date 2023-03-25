package com.yuAiTang.moxa.Db;

public class Constant {
    public static final String DB_NAME = "Db.db";
    public static final int DB_VERSION = 1;

    // 数据库变量类型
    public static final String TEXT = "TEXT"; // 变量类型 文字类
    public static final String NUMERIC = "NUMERIC"; // 变量类型 整形数字
    public static final String REAL = "REAL"; // 变量类型 浮点数字
    public static final String DATE = "DATE"; //日期
    public static final String DATETIME = "DATETIME"; //日期+时间

    public static final String TABLE_NAME_EQUIPMENT = "equipment";
    public static final String ID = "id";         // 设备ID
    public static final String STATUS = "status"; // 设备状态
    public static final String TYPE = "type";     // 艾灸设备类型
    public static final String RELAY = "relay";   // 继电器

    public static final String TABLE_NAME_RESOURCE = "resource";
//    public static final String ID = "id";
    public static final String FILENAME = "filename";
    public static final String PATH = "path";
//    public static final String TYPE = "type";

    public static final String TABLE_NAME_STATUS = "status";
//    public static final String ID = "id";
    public static final String TEMPERATURE = "temperature";
    public static final String TIME = "time";

    public static final String TABLE_NAME_SERIAL_PORT = "serial_port";
    public static final String PORT = "port";
    public static final String BAUD = "baud";
    public static final String LAST_RECORD = "last_record";
}
