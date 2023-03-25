package com.yuAiTang.moxa.entity;

public class Commands {
    // 红外测温模块
    public static final String[] S1_COMMAND = {"A5"};
    public static final String[] S1_PARAM = {"55"};
    public static final String[] S1_PARAM_2 = {"01", "03 00", "03 01", "03 02", "04", "05", "07 00", "07 01", "07 02", "A1"};

    // 64路单片机（负责调整设备使用状态、呼叫）
    public static final String[] S3_COMMAND = new String[]{"AA 60", "AA 70", "AA 76", "AA 78", "AA 79", "AA 97", "AA 98", "AA 99"};
    public static final String[] S3_PARAM = new String[]{
            "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
            "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
            "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
            "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
            "40"};
    public static final String[] S3_PARAM_2 = new String[]{"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
            "10", "11", "12", "13", "14", "15", "16", "17", "18"};

    // 32路单片机(负责开机、关机)
    public static final String[] S4_COMMAND = new String[]{"48 3A 01 57"};
    public static final String[] S4_PARAM = new String[]{"21 22 22 22", "20 22 22 22", "12 22 22 22",
            "02 22 22 22", "22 21 22 22", "22 20 22 22", "22 12 22 22", "22 02 22 22", "22 22 21 22",
            "22 22 20 22", "22 22 12 22", "22 22 02 22", "22 22 22 21", "22 22 22 20"};
    public static final String[] S4_PARAM_2 = new String[]{"22 22 22 22"};
}
