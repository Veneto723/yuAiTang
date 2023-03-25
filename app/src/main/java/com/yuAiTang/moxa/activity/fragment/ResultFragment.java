package com.yuAiTang.moxa.activity.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.entity.Attribute;
import org.jetbrains.annotations.NotNull;

public class ResultFragment extends Fragment {
    private TextView type, critic, physical, mental, illness, testTime, testId;
    private ImageView icon, qrcode;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.result, container, false);
        type = root.findViewById(R.id.type);
        critic = root.findViewById(R.id.critic);
        icon = root.findViewById(R.id.icon);
        physical = root.findViewById(R.id.physical);
        mental = root.findViewById(R.id.mental);
        illness = root.findViewById(R.id.illness);
        qrcode = root.findViewById(R.id.qrcode);
        testTime = root.findViewById(R.id.testTime);
        testId = root.findViewById(R.id.testId);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if(args != null){
            updateValues(args);
        }
    }

    public void updateValues(Bundle args){
        Attribute attr = (Attribute) args.getSerializable("attr");
        type.setText(String.format(getResources().getString(R.string.attr_name), attr.getAttr_name()));
        critic.setText(attr.getCritic());
        icon.setImageResource(attr.getIcon());
        physical.setText(String.format(getResources().getString(R.string.physical), attr.getPhysics()));
        mental.setText(String.format(getResources().getString(R.string.mental), attr.getMental()));
        illness.setText(String.format(getResources().getString(R.string.illness), attr.getIllness()));
        qrcode.setImageBitmap(args.getParcelable("qrcode"));
        testTime.setText(String.format(getResources().getString(R.string.test_time), args.getString("testTime")));
        testId.setText(String.format(getResources().getString(R.string.test_id), args.getString("testId")));

    }

    /**
     * 除了动态生成的二维码，其余全部由Bundle捆绑传递，qrcode因为是临时生成，所以可能是从服务器获取inputstream
     * 然后本地直接is转Bitmap就不在本地保存占用资源了
     * @param args 数据信息
     * @return resultFragment实例
     */
    public static ResultFragment newInstance(Bundle args){
        ResultFragment result = new ResultFragment();
        result.setArguments(args);
        return result;
    }
}
