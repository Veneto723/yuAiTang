package com.yuAiTang.moxa.util;

import android.content.Context;
import android.graphics.*;
import android.util.Base64;
import com.yuAiTang.moxa.activity.fragment.FragmentListener;
import ddf.minim.analysis.FFT;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class PictureProcess {

    private Mat lastMat = null;
    private static CascadeClassifier classifier, tongueClassifier;
    private static Rect lastRect;

    public PictureProcess(Context context) {
        OpenCVLoader.initDebug();
        classifier = new CascadeClassifier();
        tongueClassifier = new CascadeClassifier();
        String classifier_filename = "lbpcascade_frontalface.xml";
        String t_classifier_filename = "cascade_tongue.xml";
        // 将asset文件拷贝到cache下以供使用
        copyAssetAndWrite(context.getApplicationContext(), classifier_filename);
        copyAssetAndWrite(context.getApplicationContext(), t_classifier_filename);
        classifier.load(new File(context.getCacheDir(), classifier_filename).getAbsolutePath());
        tongueClassifier.load(new File(context.getCacheDir(), t_classifier_filename).getAbsolutePath());
    }

    /**
     * 将asset文件写入缓存
     */
    private void copyAssetAndWrite(Context context, String fileName) {
        try {
            File cacheDir = context.getCacheDir();
            if (!cacheDir.exists()) cacheDir.mkdirs();

            File outFile = new File(cacheDir, fileName);
            if (!outFile.exists()) outFile.createNewFile();
            else {
                if (outFile.length() > 10)//表示已经写入一次
                    return;
            }
            InputStream is = context.getAssets().open(fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buffer = new byte[is.available()];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 圆角图片
     *
     * @param map    图片[Bitmap]
     * @param radius 圆角大小 [px]
     * @return 处理后图片
     */
    public static Bitmap radius(Bitmap map, int radius) {
        Bitmap output = Bitmap.createBitmap(map.getWidth(), map.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = Color.RED;
        final Paint paint = new Paint();
        final android.graphics.Rect rect = new android.graphics.Rect(0, 0, map.getWidth(), map.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(map, rect, rect, paint);
        return output;
    }

    /**
     * 对比两张人脸相似度
     *
     * @param face1 人脸#1
     * @param face2 人脸#2
     * @return 相似程度∈(0, 1)  (不知道包不包括0、1，越大越相似)
     */
    public double compareFaces(Bitmap face1, Bitmap face2) {
        Mat faceMat1 = new Mat(face1.getHeight(), face1.getWidth(), CvType.CV_8UC4);
        Mat faceMat2 = new Mat(face2.getHeight(), face2.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(face1, faceMat1);
        Utils.bitmapToMat(face2, faceMat2);
        Imgproc.cvtColor(faceMat1, faceMat1, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(faceMat2, faceMat2, Imgproc.COLOR_RGB2HSV_FULL);
        Mat faceHist1 = new Mat();
        Mat faceHist2 = new Mat();
        MatOfInt mChannel = new MatOfInt(0, 1),
                histSize = new MatOfInt(30, 32);
        MatOfFloat range = new MatOfFloat(0, 179, 0, 255);
        Imgproc.calcHist(Collections.singletonList(faceMat1), mChannel, new Mat(), faceHist1,
                histSize, range, false);
        Imgproc.calcHist(Collections.singletonList(faceMat2), mChannel, new Mat(), faceHist2,
                histSize, range, false);
        return Imgproc.compareHist(faceHist1, faceHist2, 0);
    }

    /**
     * 判断当前帧中是否存在人脸（当前设置人脸上限为1人）
     *
     * @param map 当前帧图片
     * @return 处理后图片，人脸会被框出
     */
    public Bitmap faceDetect(Bitmap map, File directory, FragmentListener listener, AtomicBoolean flag) {
        Mat originalMat = new Mat(map.getHeight(), map.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(map, originalMat);

        // 转灰度图
        Mat mat = new Mat();
        Imgproc.cvtColor(originalMat, mat, Imgproc.COLOR_RGBA2GRAY);

        // 直方图均匀化
        Imgproc.equalizeHist(mat, mat);

        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(mat, faces, 1.1, 2, 0);
        for (Rect face : faces.toArray()) {
            if (face.x >= 80 && face.x <= 400 && face.y >= 200 && face.y <= 550) {
                Imgproc.rectangle(originalMat, face, new Scalar(0, 0, 255));
                heartBeating(originalMat.submat(face), listener);
                Bitmap newMap = Bitmap.createBitmap(originalMat.width(), originalMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(originalMat, newMap);
                if (closeEnough(face) && isNotBlur(originalMat) && !flag.get()) {
                    listener.sendValue("face-recognition:ok", null);
                    flag.set(true);
                    // save image
//                saveBitmap(newMap, directory);
                }
                lastRect = face;
                return radius(newMap, 30);
            }
        }
        return radius(map, 30);
    }

    /**
     * 检测图片中是否有舌头
     *
     * @param map      当前帧图片
     * @param listener 监听器，用于传值给Main [Activity]
     * @return 处理后图片，舌头会被框出
     */
    public Bitmap tongueDetect(Bitmap map, FragmentListener listener) {
        Mat originalMat = new Mat(map.getHeight(), map.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(map, originalMat);

        // 转灰度图
        Mat mat = new Mat();
        Imgproc.cvtColor(originalMat, mat, Imgproc.COLOR_RGBA2GRAY);

        // 直方图均匀化
        Imgproc.equalizeHist(mat, mat);

        MatOfRect tongues = new MatOfRect();
        tongueClassifier.detectMultiScale(mat, tongues, 1.1, 2, 0);
        for (Rect tongue : tongues.toArray()) {
            if (tongue.x >= 160 && tongue.x <= 320 && tongue.y >= 320 && tongue.y <= 470) {
                Imgproc.rectangle(originalMat, tongue, new Scalar(0, 0, 255));
                Bitmap newMap = Bitmap.createBitmap(originalMat.width(), originalMat.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(originalMat, newMap);
                if (closeEnough(tongue) && isNotBlur(originalMat)) {
                    try {
                        listener.sendValue("tongue-recognition:ok",
                                PictureProcess.img_2_base64(Bitmap.createBitmap(newMap, tongue.x, tongue.y, tongue.width, tongue.height)));
                    } catch (IOException ignored) {
                    }
                }
                lastRect = tongue;
                return radius(newMap, 30);
            }
        }
        return radius(map, 30);
    }

    /**
     * 判断脸部矩形是否足够接近，专用于faceDetect()方法
     *
     * @param rect 脸部区块
     * @return 是否足够接近
     */
    private boolean closeEnough(Rect rect) {
        if (lastRect != null) {
            return Math.abs(rect.x - lastRect.x) <= 20 && Math.abs(rect.y - lastRect.y) <= 20 &&
                    Math.abs(rect.height - lastRect.height) <= 20 &&
                    Math.abs(rect.width - lastRect.width) <= 20;
        }
        return false;
    }

    /**
     * 判断图片是否模糊，现在这个功能，hmmm,不是很行
     *
     * @param mat 待判断Mat图片
     * @return true不模糊，false模糊
     */
    public boolean isNotBlur(Mat mat) {
        Mat gray = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1);
        Mat lap = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC4);
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Laplacian(gray, lap, 3);
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(lap, new MatOfDouble(), std);
        return std.toArray()[0] > 12;
    }


    private final int BUFFER_SIZE = 32;
    private ArrayList<Float> pool = new ArrayList<>();
    private final FFT fft = new FFT(BUFFER_SIZE, BUFFER_SIZE);

    /**
     * 通过图片测心率
     * 参考文档：
     * [1] https://github.com/pishangujeniya/FaceToHeart
     * [2] https://stackoverflow.com/questions/27310426/detecting-heartbeat-using-webcam
     *
     * @param mat      每一帧图片
     * @param listener Fragment监听器
     */
    public void heartBeating(Mat mat, FragmentListener listener) {
        ArrayList<Mat> bgr = new ArrayList<>();
        Core.split(mat, bgr);
        Scalar scalar = Core.sumElems(bgr.get(1));
        float green_avg = (float) (scalar.val[0] / 3) / (mat.rows() * mat.cols());
        if (pool.size() < BUFFER_SIZE) {
            pool.add(green_avg);
        } else pool.remove(0);
        float[] sample = new float[pool.size()];
        for (int i = 0; i < pool.size(); i++) sample[i] = pool.get(i);

        if (sample.length >= BUFFER_SIZE) {
            fft.forward(sample, 0);
            float frequency = 0;
            for (int i = 0; i < fft.specSize(); i++) frequency = Math.max(frequency, fft.getBand(i));
            listener.sendValue("bpm-recognition:ok", (int) (frequency * fft.getBandWidth() / 60) * 4 + 5);
            pool = new ArrayList<>();
        }
    }


    /**
     * 动态捕捉，捕获当前帧是否有物体移动
     *
     * @param map 当前图片
     * @return 是否有移动物体存在
     */
    public boolean movementDetect(Bitmap map) {
        Mat originalMat = new Mat(map.getHeight(), map.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(map, originalMat);

        // 转灰度图
        Mat mat = new Mat();
        Imgproc.cvtColor(originalMat, mat, Imgproc.COLOR_RGBA2GRAY);

        // 高斯模糊
        Imgproc.GaussianBlur(mat, mat, new Size(21, 21), 0);

        // 求差值
        Mat frameDelta = new Mat();
        if (lastMat == null) {
            lastMat = mat;
            return false;
        }
        Core.absdiff(lastMat, mat, frameDelta);

        // 膨胀
        Imgproc.dilate(frameDelta, frameDelta, new Mat(), new Point(-1, -1), 2);

        // 二值化处理
        Imgproc.threshold(frameDelta, frameDelta, 25, 255, Imgproc.THRESH_BINARY);

        // 检测边框
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(frameDelta, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint point : contours)
            if (Imgproc.contourArea(point) > 2000) return true;
        return false;
    }

    /**
     * 保存图片到本地
     *
     * @param map 待保存图片
     */
    private static void saveBitmap(Bitmap map, File directory) {
        directory.mkdir();
        File saveFile = new File(directory.getPath(), UUID.randomUUID().toString() + ".jpg");
        try {
            FileOutputStream os = new FileOutputStream(saveFile);
            map.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 将Bitmap转换为base64编码
     *
     * @param map 待转换图片
     * @return base64字符串
     * @throws IOException 转换成ByteArrayOutputStream出bug了
     */
    public static String img_2_base64(Bitmap map) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        map.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.flush();
        os.close();
        return Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }
}
