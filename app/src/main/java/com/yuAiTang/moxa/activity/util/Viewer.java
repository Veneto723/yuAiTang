package com.yuAiTang.moxa.activity.util;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import com.yuAiTang.moxa.entity.Resource;
import com.yuAiTang.moxa.util.Utils;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Viewer {

    private final LinkedList<String> paths;
    private final ImageView imageView;
    private final VideoView videoView;
    private final AtomicInteger currentFrame = new AtomicInteger();
    private int currentTotalFrame = -1;
    private boolean isVideo = false;
    private boolean detecting = false;
    private boolean ultimateFlag = true;

    public Viewer(LinkedList<String> paths, ImageView image, VideoView video) {
        this.paths = paths;
        this.imageView = image;
        this.videoView = video;
    }

    public void appendResource(String path) {
        this.paths.add(path);
    }

    public void appendResources(LinkedList<String> paths) {
        this.paths.addAll(paths);
    }

    public void clearResources() {
        this.paths.clear();
    }

    public void start() {
        loop.start();
    }

    public void pause() {
        detecting = true;
    }

    public void resume() {
        detecting = false;
        if (isVideo) {
            videoView.seekTo(currentFrame.get());
            videoView.start();
        }
    }

    public void stop(){
        ultimateFlag = false;
        loop.interrupt();
    }

    public void zoomIn() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        if (videoView != null) videoView.setLayoutParams(layoutParams);
        if (imageView != null) imageView.setLayoutParams(layoutParams);
    }

    public void zoomOut() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(1680, 945);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        if (videoView != null) videoView.setLayoutParams(layoutParams);
        if (imageView != null) imageView.setLayoutParams(layoutParams);
    }

    Thread loop = new Thread() {
        int index = 0;

        public void run() {
            while (ultimateFlag) {
                if (!detecting) {
                    if (index >= paths.size()) index = 0;
                    String path = paths.get(index);
                    if (currentTotalFrame == -1) { // 新Video or image
                        Message msg = handler.obtainMessage();
                        Bundle args = new Bundle();
                        args.putString("path", path);
                        msg.setData(args);
                        handler.sendMessage(msg);
                        isVideo = Resource.isVideo(path);
                        if (isVideo) {
                            try {
                                currentTotalFrame = videoView.getDuration();
                            }catch (IllegalStateException e){
                                e.printStackTrace();
                            }
                        } else {
                            currentTotalFrame = 15 * 1000;
                            currentFrame.set(0);
                        }
                    } else if ((isVideo && videoView.isPlaying()) ||
                            (!isVideo && currentFrame.get() < currentTotalFrame)) { // 正在播放的Video or Image
                        if (isVideo) {
                            currentFrame.set(videoView.getCurrentPosition());
                        } else {
                            currentFrame.set(currentFrame.get() + 1000);
                        }
                    } else { // 播放完的Video or Image
                        index++;
                        currentTotalFrame = -1;
                        currentFrame.set(0);
                        continue;
                    }
                } else {
                    if (isVideo && videoView.canPause()) videoView.pause();
                }
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    };

    Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.getData() != null) {
                String path = msg.getData().getString("path");
                if (isVideo) {
                    videoView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                    videoView.setVideoURI(Uri.parse(path));
                    videoView.start();
                } else {
                    imageView.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                    Bitmap img = Controller.getBitmap(path);
                    if (img == null) {
                        img = Controller.addBitmap(Utils.getLocalImg(path), path);
                    }
                    imageView.setImageBitmap(img);
                }
            }

        }
    };
}
