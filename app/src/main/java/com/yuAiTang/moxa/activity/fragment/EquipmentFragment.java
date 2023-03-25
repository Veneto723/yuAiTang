package com.yuAiTang.moxa.activity.fragment;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.entity.Equipment;

import java.lang.ref.WeakReference;

public class EquipmentFragment extends Fragment {

    private static final int[] BACKGROUNDS = new int[]{R.drawable.crimson_background, R.drawable.yellow_background,
            R.drawable.lime_backgound};
    private static final int[] EQUIP_TYPE = new int[]{R.drawable.equip_1_white, R.drawable.equip_2_white, R.drawable.equip_3_white};
    private static final String[] EQUIP_TEXT = new String[]{"使用中", "已预约", "空闲中"};

    private TextView facilityName, statusText;
    private ImageView equipIcon, statusIndicator;
    private RelativeLayout rootView;
    private boolean isCalling = false;
    private Equipment equip;
    private Handler handler;

    /**
     * 该设备进行呼叫， View开始振动并更改图标
     */
    public void call() {
        isCalling = true;
        statusText.setText(getResources().getString(R.string.calling));
        statusIndicator.setImageResource(R.drawable.ringing_bell_white);
        rootView.setBackgroundResource(R.drawable.orange_background);
    }

    /**
     * 该设备进行取消呼叫， View暂停振动并更改图标
     */
    public void cancel() {
        isCalling = true;
        statusText.setText(EQUIP_TEXT[equip.getStatus()]);
        statusIndicator.setImageResource(R.drawable.bubble);
        rootView.setBackgroundResource(BACKGROUNDS[equip.getStatus()]);
    }

    public void changeStatus(int status) {
        equip.setStatus(status);
        statusText.setText(EQUIP_TEXT[equip.getStatus()]);
        rootView.setBackgroundResource(BACKGROUNDS[equip.getStatus()]);
    }


    private static class equipHandler extends Handler {
        WeakReference<EquipmentFragment> equipFrag;

        public equipHandler(EquipmentFragment frag, @NonNull Looper looper) {
            super(looper);
            equipFrag = new WeakReference<>(frag);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EquipmentFragment notify = equipFrag.get();
            int delta = 5;
            PropertyValuesHolder waving = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                    Keyframe.ofFloat(0f, 0),
                    Keyframe.ofFloat(.10f, -delta),
                    Keyframe.ofFloat(.26f, delta),
                    Keyframe.ofFloat(.42f, -delta),
                    Keyframe.ofFloat(.58f, delta),
                    Keyframe.ofFloat(.74f, -delta),
                    Keyframe.ofFloat(.90f, delta),
                    Keyframe.ofFloat(1f, 0f));
            ObjectAnimator.ofPropertyValuesHolder(notify.rootView, waving).setDuration(500).start();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.equipment, container, false);
        equipIcon = root.findViewById(R.id.equipIcon);
        facilityName = root.findViewById(R.id.facilityName);
        statusIndicator = root.findViewById(R.id.statusIndicator);
        statusText = root.findViewById(R.id.statusText);
        rootView = root.findViewById(R.id.root);
        handler = new equipHandler(this, Looper.myLooper());
        if (getActivity() instanceof FragmentListener) {
            FragmentListener listener = (FragmentListener) getActivity();
            root.setOnClickListener(v -> listener.sendValue(getTag(), "open"));
        }
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if (args != null) {
            equip = (Equipment) args.getSerializable("equipment");
            equipIcon.setImageResource(EQUIP_TYPE[equip.getType()]);
            facilityName.setText(String.format(getResources().getString(R.string.facilityName), equip.getId()));
            if(equip.getStatus() == -1){
                statusText.setText(EQUIP_TEXT[0]);
                rootView.setBackgroundResource(BACKGROUNDS[0]);
            }else{
                statusText.setText(EQUIP_TEXT[equip.getStatus()]);
                rootView.setBackgroundResource(BACKGROUNDS[equip.getStatus()]);
            }
            statusIndicator.setImageResource(R.drawable.bubble);
        }
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (isCalling)
                        handler.sendMessage(handler.obtainMessage());
                    try {
                        sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }.start();
    }


    public static EquipmentFragment newInstance(Equipment equipment) {
        EquipmentFragment equipmentFragment = new EquipmentFragment();
        Bundle args = new Bundle();
        args.putSerializable("equipment", equipment);
        equipmentFragment.setArguments(args);
        return equipmentFragment;
    }
}
