package com.yuAiTang.moxa.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.yuAiTang.moxa.activity.util.Controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    // Digit manipulate
    /**
     * Pixel值转density-independent pixel值
     *
     * @param context 上下文，无意义。用于获取density
     * @param pxValue 待转换pixel值
     * @return 转换后dp值
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * Density-independent pixel值转Pixel值
     *
     * @param context 上下文，无意义。用于获取density
     * @param dpValue 待转换dp值
     * @return 转后后pixel值
     */
    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    // Fetcher系列

    /**
     * 根据Uri获取文件路径
     *
     * @param context 上下文，无意义
     * @param uri     文件Uri
     * @return 文件路径
     */
    public static String getFilePathByUri(Context context, Uri uri) {
        String path;
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.parseLong(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        } else {
            // 以 file:// 开头的
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                path = uri.getPath();
                return path;
            }
        }
        return null;
    }

    // 与getFilePathByUri配合使用
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    // 与getFilePathByUri配合使用
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    // 与getFilePathByUri配合使用
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    // 与getFilePathByUri配合使用
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * 获取本机MAC地址
     *
     * @return mac地址
     * @throws SocketException exception, you need to handle it
     */
    public static String getMac() throws SocketException {
        ArrayList<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface nif : all) {
            if (!nif.getName().equals("wlan0"))
                continue;
            byte[] macBytes = nif.getHardwareAddress();
            if (macBytes == null) return "";
            StringBuilder res1 = new StringBuilder();
            for (Byte b : macBytes) {
                res1.append(String.format("%02X:", b));
            }
            if (!TextUtils.isEmpty(res1)) {
                res1.deleteCharAt(res1.length() - 1);
            }
            return res1.toString();
        }
        return "";
    }

    /**
     * 以Bitmap形式获取存在本地的图片
     *
     * @param location 图片文件路径
     * @return 图片Bitmap
     */
    public static Bitmap getLocalImg(String location) {
        String name = new File(location).getName();
        if (Controller.notExistBitmap(name)) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(location);
            } catch (FileNotFoundException ignored) {
            }
            return Controller.addBitmap(BitmapFactory.decodeStream(is), name);
        } else {
            return Controller.getBitmap(name);
        }
    }

    /**
     * 获取当前软件版本
     *
     * @param context 上下文，无意义
     * @return 版本号
     * @throws PackageManager.NameNotFoundException exception, you need to handle it
     */
    public static String getAppVersion(Context context) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = context.getApplicationContext().getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
        return packageInfo.versionName;
    }

    /**
     * 获取十位Unix时间戳
     *
     * @return 时间戳
     */
    public static int getUnixTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    /**
     * 获取当前时间
     *
     * @return 时间，格式 2020年12月18日 下午3:36:22
     */
    public static String getCurrentTime() {
        return SimpleDateFormat.getDateTimeInstance().format(new Date());
    }

    /**
     * 获取本机IP地址
     *
     * @return ip地址，获取失败则返回 0.0.0.0
     * @throws SocketException exception, you need to handle it
     */
    public static String getIP() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        return "0.0.0.0";
    }

    // 加密


    public static String md5(String msg){
        char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9', 'A','B','C','D','E','F'};
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(msg.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md5.digest();
            // 首先初始化一个字符数组，用来存放每个16进制字符
            // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
            char[] resultCharArray =new char[digest.length * 2];
            // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
            int index = 0;
            for (byte b : digest) {
                resultCharArray[index++] = hexDigits[b>>> 4 & 0xf];
                resultCharArray[index++] = hexDigits[b& 0xf];
            }
            // 字符数组组合成字符串返回
            return new String(resultCharArray).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String nonce_str(){
        return UUID.randomUUID().toString();
    }
}
