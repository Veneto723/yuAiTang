package com.yuAiTang.moxa.entity;


public enum Tracks {
    boot, reboot, manual, enter, // 设备启动、重启、进入手动调试模式、进入后台管理系统
    command_pool, serialPort, // 指令池、串口指令
    internet, upload, upgrade, // 网络连接、上传日志、升级
    debug, whatever, broadcast, // 报错，随意内容(主要用于调试)
}
