package com.yuAiTang.moxa.entity;

public class SerialPort {
    private String port; // 串口地址
    private Integer baud; // 串口波特率
    private String lastRecord; // 串口最后握手时间

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Integer getBaud() {
        return baud;
    }

    public void setBaud(Integer baud) {
        this.baud = baud;
    }

    public String getLastRecord() {
        return lastRecord;
    }

    public void setLastRecord(String lastRecord) {
        this.lastRecord = lastRecord;
    }
}
