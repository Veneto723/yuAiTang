package com.yuAiTang.moxa.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.yuAiTang.moxa.entity.Tracks;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class IConnector {

    // HTTP 访问方法
    public enum Method {
        GET, // 返回String
        GET_STREAM, // 返回InputStream
        GET_IMAGE, // 返回Bitmap
        POST, // 返回String
        POST_STREAM, // 返回InputStream
        POST_WORESP, // 无返回
        UPLOAD, // 上传文件
    }
    
    final private String url; // 网址
    private String argStr = ""; // 参数
    private File file;
    private HttpURLConnection conn;

    /**
     * 返回伪URL和参数合集用于检查使用
     * @return [String] URL变量+argStr变量
     */
    public String getPseudoURL(){
        return url + argStr;
    }

    /**
     * HTTP无需传递参数的构造方法
     * @param url url
     */
    public IConnector(String url){
        this.url =  url;
    }

    /**
     * HTTP需要传递参数的构造方法
     * @param url url
     * @param args 参数Map键值对，String类型，
     */
    public IConnector(String url, String args) {
        this.url = url;
        this.argStr = args;
    }

    /**
     * HTTP需要上传文件及参数的构造方法
     * @param url url
     * @param args 参数列表
     * @param file 上传的文件对象
     */
    public IConnector(String url, String args, File file){
        this.url = url;
        this.argStr = args;
        this.file = file;
    }


    /**
     * 对URL采取GET访问方式
     * @return 访问获取的返回数据流
     */
    private InputStream getStream() throws IOException{
        URL url = new URL(this.url + argStr);
        conn = (HttpURLConnection) url.openConnection();
        return conn.getInputStream();
    }

    /**
     * 对URL采取GET访问方式
     * @return 访问获取的返回数据流将在方法里直接转换为Bitmap格式
     */
    private Bitmap getImage() throws IOException{
        return BitmapFactory.decodeStream(getStream());
    }

    /**
     * 对URL采取POST访问方式
     * @return 访问获取的输入流
     */
    private InputStream postStream() throws IOException{
        URL url = new URL(this.url);
        conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        // TODO 可更换为 application/x-www-form-urlencode
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8;");
        if(argStr != null) {
            if (argStr.length() > 0) { // 表示有参数需要传递
                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
                out.append(argStr);
                out.flush();
                out.close();
            }
        }
        conn.connect();
        return conn.getInputStream();
    }

    /**
     * 采用POST进行上传文件
     * @return HTTP访问返回的输入流信息
     */
    private InputStream uploadFile() throws IOException{
        String boundary = "---------------------------"; // 分割线
        URL url = new URL(this.url); // 用来开启连接

        // 将开头和结尾部分转为字节数组，因为设置Content-Type时长度是字节长度
        String builder = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + "\r\n" +
                "Content-Type: application/octet-stream" + "\r\n" + "\r\n";
        byte[] before = builder.getBytes(StandardCharsets.UTF_8);
        byte[] after = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        // 打开连接, 设置请求头
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Content-Length", before.length + file.length() + after.length + "");
        conn.setDoOutput(true);
        // 获取输入输出流
        OutputStream out = conn.getOutputStream();
        FileInputStream fis = new FileInputStream(file);
        // 将开头部分写出
        out.write(before);
        // 写出文件数据
        byte[] buffer = new byte[fis.available()];
        int len;
        while ((len = fis.read(buffer)) != -1)
            out.write(buffer, 0, len);

        // 将结尾部分写出
        out.write(after);
        fis.close();
        out.close();
        return conn.getInputStream();
    }

    /**
     * 对URL采取POST访问方式，但不关心返回值
     */
    private void postWOResp() throws IOException{
        postStream();
    }


    /**
     * HTTP请求，每次请求将至多持续3s、失败3次
     * @param context 上下文，无意义
     * @param type 请求类型
     * @return 请求返回结果
     */
    public Object hyperReq(Context context, Method type){
        return hyperReq(context, type, 3);
    }

    /**
     * HTTP请求，请求将至多持续5s或失败times次
     * @param context 上下文，无意义
     * @param type 请求类型
     * @param fail_times 至多失败次数
     * @return 请求返回结果
     */
    public Object hyperReq(Context context, Method type, int fail_times){
        Object[] indicator = {0, false}; // 访问是否成功
        Object[] resp = {null};
        Tracker.track(context, Tracks.internet, "开始网络请求[" + type + "]， Pseudo url:" + getPseudoURL());
        Thread reqThread = new Thread(){
            @Override
            public void run() {
                while((int)indicator[0] < fail_times && !(boolean)indicator[1]){
                    try {
                        switch (type) {
                            case GET:  resp[0] = converter(getStream());
                                break;
                            case GET_STREAM: resp[0] = getStream();
                                break;
                            case GET_IMAGE: resp[0] = getImage();
                                break;
                            case POST: resp[0] = converter(postStream());
                                break;
                            case POST_STREAM: resp[0] = postStream();
                                break;
                            case POST_WORESP: postWOResp();
                                break;
                            case UPLOAD: resp[0] = converter(uploadFile());
                                break;
                        }
                        if(resp[0] == null && type != Method.POST_WORESP){
                            indicator[0] = (int) indicator[0] + 1;
                            indicator[1] = false;
                            // 请求方式声明了会有返回值，但是返回值为空。判定因为网络问题，请求失败
                            // 这边的返回值是数据流形式
                            Tracker.track(context, Tracks.internet, "请求方式声明了会有返回值，但是返回值为空。判定因为网络问题，请求失败" +
                                    indicator[0] + "次，RAW_RESP:" + resp[0]);
                        }else if(type == Method.GET || type == Method.POST){
                            if(((String)resp[0]).length() == 0){
                                indicator[0] = (int) indicator[0] + 1;
                                indicator[1] = false;
                                // 请求方式声明了会有返回值，但是返回值为空。判定因为网络问题，请求失败
                                // 这里的返回值的字符串形式
                                Tracker.track(context, Tracks.internet, "请求方式声明了会有返回值，但是返回值为空。判定因为网络问题，请求失败" +
                                        indicator[0] + "次，RAW_RESP:" + resp[0]);
                            }else{
                                indicator[1] = true;
                                Tracker.track(context, Tracks.internet, "请求成功，RAW_RESP:" + resp[0]);
                                interrupt();
                            }
                        }else{
                            indicator[1] = true;
                            Tracker.track(context, Tracks.internet, "请求成功，RAW_RESP:" + resp[0]);
                            interrupt();
                        }
                    }catch (IOException e){
                        // 因为网络问题，导致访问不成功
                        indicator[0] = (int) indicator[0] + 1;
                        indicator[1] = false;
                        Tracker.track(context, Tracks.internet, "因为网络问题，导致报错+访问不成功，请求失败" +
                                indicator[0] + "次，RAW_RESP:" +resp[0] + "\n" + e.toString());
                    }
                }
            }
        };
        try{
            reqThread.start();
            reqThread.join(5 * 1000); // 访问将一共持续至多5s
            reqThread.interrupt();
        } catch (InterruptedException ignored) { }
        if(conn != null && type != Method.POST_STREAM && type != Method.GET_STREAM) {
            // 如果是输入流返回的话，本地后续可能会对数据流进行操作（保存、转义、上传），
            // 所以在这不能直接关闭访问（关闭了，数据流也就空了）
            conn.disconnect();
        }
        return resp[0];
    }

    public void disconnect(){
        if(conn != null) conn.disconnect();
    }


    /**
     * 将参数Map集合转换成string对象，类型 key=value&key(n)=value(n)
     * @param args 参数Map集合
     * @param <T> 参数的值类型可任意，但是会被强制转换成String
     * @return 转换后的HTTP参数列表
     */
    public static <T> String args2Str(Map<String,T> args){
        StringBuilder str = new StringBuilder("?");
        for(String key: args.keySet()){
            str.append(key).append("=").append(args.get(key)).append("&");
        }
        // 因为本来就要去掉多余的“&”符号，再加之如果args列表是空的的话，就不需要？直接返回空字符串。正好就可以通过substring()直接一起处理
        return str.substring(0, str.length() - 1);
    }

    /**
     * 将HTTP请求返回的InputStream转换成String
     * @param is HTTP返回的输入流
     * @return 由输入流转换成的String返回值
     */
    private static String converter(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder resp = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null){
            resp.append(line);
        }
        return resp.toString();
    }
}
