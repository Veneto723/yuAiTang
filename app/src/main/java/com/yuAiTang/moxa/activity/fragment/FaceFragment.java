package com.yuAiTang.moxa.activity.fragment;

import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.activity.util.CameraUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;

public class FaceFragment extends Fragment {
    private ImageView preview, faceContour, tongueContour;
    private TextView alert1, alert2;
    private int cameraId = 0;
    private FragmentListener listener;

    public void hideExtras() {
        faceContour.setVisibility(View.GONE);
        tongueContour.setVisibility(View.GONE);
    }

    public void changeAlert(String changeTo) {
        alert1.setText(changeTo);
        alert2.setText(changeTo);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.face, container, false);
        preview = root.findViewById(R.id.preview);
        faceContour = root.findViewById(R.id.face);
        tongueContour = root.findViewById(R.id.tongue);
        alert1= root.findViewById(R.id.upperAlert);
        alert2 = root.findViewById(R.id.lowerAlert);
        if (getActivity() instanceof FragmentListener) {
            listener = (FragmentListener) getActivity();
        }
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if (args != null) {
            cameraId = args.getInt("cameraId", 0);
        }
    }

    public static class FaceHandler extends Handler {
        private final WeakReference<FaceFragment> face;
        private CameraUtil util;

        public FaceHandler(FaceFragment face, Looper looper) {
            super(looper);
            this.face = new WeakReference<>(face);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle data = msg.getData();
            int type = data.getInt("type", 0);
            FaceFragment face = this.face.get();
            try {
                switch (type) {
                    case 0: // 开始脸部检测
                        if (util != null) util.stopPreview();
                        util = new CameraUtil(face.getContext(), String.valueOf(face.cameraId), face.preview, (FrameLayout) msg.obj,
                                0, new File("/sdcard/faces"), face.listener);
                        util.startPreview(face.getContext());
                        face.faceContour.setVisibility(View.VISIBLE);
                        face.tongueContour.setVisibility(View.GONE);
                        face.alert1.setText("请将脸部停留在蓝色指示框内");
                        face.alert2.setText("请将脸部停留在蓝色指示框内");
                        break;
                    case 1: // 暂停预览
                        face.alert1.setText("检测结束");
                        face.alert2.setText("检测结束");
                        if (util != null) util.stopPreview();
                        face.hideExtras();
                        break;
                    case 2: // 开始舌部检测
                        if (util != null) util.stopPreview();
                        util = new CameraUtil(face.getContext(), String.valueOf(face.cameraId), face.preview, (FrameLayout)msg.obj, 0, face.listener);
                        util.startPreview(face.getContext());
                        face.tongueContour.setVisibility(View.VISIBLE);
                        face.faceContour.setVisibility(View.GONE);;
                        face.alert1.setText("请将舌头停留在蓝色指示框内");
                        face.alert2.setText("请将舌头停留在蓝色指示框内");
                        break;
                    case -1:
                        face.preview.setImageBitmap(null);
                        break;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static FaceFragment newInstance() {
        return new FaceFragment();
    }

    public static FaceFragment newInstance(int cameraId) {
        FaceFragment face = new FaceFragment();
        Bundle args = new Bundle();
        args.putInt("cameraId", cameraId);
        face.setArguments(args);
        return face;
    }
}
