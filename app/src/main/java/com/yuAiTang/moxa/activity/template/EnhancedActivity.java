package com.yuAiTang.moxa.activity.template;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.yuAiTang.moxa.activity.util.Controller;

public class EnhancedActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Controller.addActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Controller.removeActivity(this);
    }
}
