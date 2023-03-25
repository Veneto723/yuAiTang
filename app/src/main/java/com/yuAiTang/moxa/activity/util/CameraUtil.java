package com.yuAiTang.moxa.activity.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.yuAiTang.moxa.activity.fragment.FragmentListener;
import com.yuAiTang.moxa.util.PictureProcess;
import com.yuAiTang.moxa.util.ThreadPool;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraUtil {

    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private final ImageReader reader = ImageReader.newInstance(1600, 900, ImageFormat.JPEG, 2);
    private boolean isPreview = false;
    private boolean loopFaceDetect = true;
    private final String cameraId;
    private final AtomicBoolean passFlag = new AtomicBoolean(false);

    /**
     * 仅做预览使用的CameraUtil
     *
     * @param prototype 预览用ImageView
     */
    public CameraUtil(String cameraId, ImageView prototype) {
        this.cameraId = cameraId;
        reader.setOnImageAvailableListener(reader -> {
            // 拿到照片数据
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = img2Bitmap(image);
                prototype.setImageBitmap(bitmap);
                image.close();
            }
        }, null);
    }

    /**
     * 预览+ initialDelay (ms) 后自动拍照人脸检测
     *
     * @param cameraId     摄像头ID
     * @param prototype    预览用ImageView
     * @param loading      加载用View
     * @param initialDelay 延迟
     * @param directory    人脸图片存储目录
     * @param listener     Fragment监听器用于向主Activity传输反馈
     */
    public CameraUtil(Context context, String cameraId, ImageView prototype, FrameLayout loading, long initialDelay,
                      File directory, FragmentListener listener) {
        this.cameraId = cameraId;
        PictureProcess process = new PictureProcess(context);
        AtomicBoolean flag = new AtomicBoolean(false);
        // initialDelay(毫秒)的定时器
        ThreadPool.scheduleWithDelay(() -> {
            if (loopFaceDetect) flag.set(true);
        }, initialDelay, 100);
        reader.setOnImageAvailableListener(reader -> {
            loading.setVisibility(View.GONE);
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = img2Bitmap(image);
                prototype.setImageBitmap(PictureProcess.radius(bitmap, 30));
                if (flag.get()) {
                    prototype.setImageBitmap(process.faceDetect(bitmap, directory, listener, this.passFlag));
                    flag.set(false);
                }
                image.close();
            }
        }, null);
    }

    /**
     * 预览 + initialDelay (ms) 后自动拍照舌头检测
     *
     * @param cameraId     摄像头ID
     * @param prototype    预览用ImageView
     * @param loading      加载用View
     * @param initialDelay 延迟
     * @param listener     Fragment监听器用于向主Activity传输反馈
     */
    public CameraUtil(Context context, String cameraId, ImageView prototype, FrameLayout loading, long initialDelay,
                      FragmentListener listener) {
        this.cameraId = cameraId;
        PictureProcess process = new PictureProcess(context);
        AtomicBoolean flag = new AtomicBoolean(false);
        // initialDelay(毫秒)的定时器
        ThreadPool.scheduleWithDelay(() -> {
            if (loopFaceDetect) flag.set(true);
        }, initialDelay, 1000);
        reader.setOnImageAvailableListener(reader -> {
            loading.setVisibility(View.GONE);
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = img2Bitmap(image);
                prototype.setImageBitmap(PictureProcess.radius(bitmap, 30));

                if (flag.get()) {
                    prototype.setImageBitmap(process.tongueDetect(bitmap, listener));
                    flag.set(false);
                }
                image.close();
            }
        }, null);
    }

    public void startPreview(Context ctx) throws CameraAccessException, IllegalArgumentException {
        if (!isPreview) {
            CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    try {
                        // 目前的SDK版本还未过期，新版的SDK版本又高了
                        mCameraDevice.createCaptureSession(Collections.singletonList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCameraCaptureSession = session;
                                try {
                                    CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    builder.addTarget(reader.getSurface());
                                    isPreview = true;
                                    passFlag.set(false);
                                    mCameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                isPreview = false;
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    isPreview = false;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    isPreview = false;
                    Log.d("摄像头预览", camera.toString() + "=>Error:" + error);
                }
            }, null);
        }
    }

    public void stopPreview() throws CameraAccessException {
        if (mCameraCaptureSession != null && isPreview) mCameraCaptureSession.stopRepeating();
        if (mCameraDevice != null && isPreview) mCameraDevice.close();
        loopFaceDetect = false;
        isPreview = false;

    }

    private static Bitmap img2Bitmap(Image image) {
        ByteBuffer buffer = Objects.requireNonNull(image).getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);//由缓冲区存入字节数组
        return PictureProcess.rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length), 90);
    }
}
