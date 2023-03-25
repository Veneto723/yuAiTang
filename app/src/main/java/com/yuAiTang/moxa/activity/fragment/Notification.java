package com.yuAiTang.moxa.activity.fragment;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.R;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Notification extends Fragment {
    private TextView info, time, date, temperature;
    private ImageView internet;
    private Animation animation;
    long[] mHits = new long[5]; // 点击次数
    private boolean isStop = false;
    private static final int[] INTERNET_INDICATORS = new int[]{R.drawable.wifi_none, R.drawable.wifi_low,
            R.drawable.wifi_moderate, R.drawable.wifi_full, R.drawable.cable};
    private static notifyHandler handler;
    private SQLiteDatabase db;

    public static notifyHandler getHandler() {
        return handler;
    }

    public void setInternet(int id) {
        internet.setImageResource(INTERNET_INDICATORS[id]);
    }

    public void setInfo(String info) {
        if (this.info != null) {
            this.info.setText(info);
            stopAnimation();
            startAnimation();

        }
    }


    public void setDateAndTime(Date now) {
        String nowStr = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(now);
        if (this.date != null) {
            this.date.setText(nowStr.split(" ")[0]);
        }
        if (this.time != null) {
            String rawStr = nowStr.split(" ")[1];
            String hour = rawStr.substring(2).split(":")[0];
            hour = hour.length() == 1 ? "0" + hour : hour;
            String timeStr = rawStr.substring(0, 2) + "：" + hour + ":" + rawStr.substring(2).split(":")[1];
            this.time.setText(timeStr);
        }
    }

    public void setTemperature(double temperature) {
        this.temperature.setText(String.format(getResources().getString(R.string.temperature), temperature));
    }

    public void startAnimation() {
        int xDelta = -(info.getPaddingStart() + info.getText().length() * (int) info.getTextSize());
        animation = new TranslateAnimation(Animation.ABSOLUTE, xDelta, Animation.ABSOLUTE, 0);
        animation.setDuration(60 * 1000);
        animation.setRepeatCount(Animation.INFINITE);
        info.startAnimation(animation);
    }

    public void stopAnimation() {
        if (animation != null)
            animation.cancel();
    }

    public static class notifyHandler extends Handler {
        WeakReference<Notification> notifyFrag;

        public notifyHandler(Notification frag, @NonNull Looper looper) {
            super(looper);
            notifyFrag = new WeakReference<>(frag);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Notification notify = notifyFrag.get();
            if (!notify.isStop) {
                Bundle args = msg.getData();
                if (args != null) {
                    String type = args.getString("type");
                    switch (type) {
                        case "notify":
                            notify.setInfo(args.getString("info"));
                            break;
                        case "timeUtil":
                            notify.setDateAndTime(new Date());
                            break;
                        case "temperature":
                            if (notify.db != null)
                                notify.setTemperature(Connector.getLatestTemperature(notify.db));
                            break;
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.notification, container, false);
        info = root.findViewById(R.id.info);
        date = root.findViewById(R.id.date);
        time = root.findViewById(R.id.time);
        temperature = root.findViewById(R.id.temperature);
        internet = root.findViewById(R.id.internet);
        handler = new notifyHandler(this, Looper.myLooper());
        db = Connector.getDb(root.getContext());
        // 拦截ScrollView的滑动事件，毕竟我们是依靠ScrollView才能保持文本不会被截断
        root.findViewById(R.id.root).setOnTouchListener((v, event) -> true);
        // fragment listener
        if (getActivity() instanceof FragmentListener) {
            FragmentListener listener = (FragmentListener) getActivity();
            root.findViewById(R.id.microphone).setOnClickListener(view -> {
                // 连续点击5次进入管理员后台
                System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
                mHits[mHits.length - 1] = SystemClock.uptimeMillis();
                if (SystemClock.uptimeMillis() - mHits[0] <= 1500) {
                    mHits = new long[5];
                    listener.sendValue("enter", null);
                }
            });
        }
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        isStop = false;
        Bundle args = getArguments();
        if (args != null) {
            setInfo(args.getString("info"));
            startAnimation();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        isStop = true;
    }


    public static Notification newInstance(String info) {
        Notification notification = new Notification();
        Bundle args = new Bundle();
        args.putString("info", info);
        notification.setArguments(args);
        return notification;
    }
}
