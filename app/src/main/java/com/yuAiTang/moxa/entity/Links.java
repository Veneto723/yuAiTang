package com.yuAiTang.moxa.entity;

public class Links {

    public static final String JAVA_SERVER = "http://106.14.214.15:8080/server";

    public static final String APK = JAVA_SERVER + "/apk";              // 检测APK版本
    public static final String EQUIP_MANI = JAVA_SERVER + "/equipMani"; // 设备管理（设备增/删/改）
    public static final String FACE = JAVA_SERVER + "/faceDetect";      // 脸部检测接口
    public static final String TONGUE = JAVA_SERVER + "/tongueDetect";  // 舌头检测
    public static final String LOG = JAVA_SERVER + "/log";              // 上传日志

    public static final String WECHAT_SERVER = "http://www.penetratechain.com";

    public static final String MOX_INFO = WECHAT_SERVER + "/api/Moxibustion/moxInfo"; // 同步/初始化接口
    public static final String MOX_STATUS = WECHAT_SERVER + "/api/Moxibustion/moxStatus"; // 艾灸设备状态
    public static final String SUBMIT_OFF = WECHAT_SERVER + "/api/Moxibustion/submit_off"; // 门禁

    // /api/Moxibustion/checkUser 意义不明的接口
    // /api/Moxibustion/action_curl_pos
    // /api/Moxibustion/userTemplete
}
