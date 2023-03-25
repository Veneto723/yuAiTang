package com.yuAiTang.moxa.activity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.activity.fragment.DetailedEquipment;
import com.yuAiTang.moxa.activity.fragment.FragmentListener;
import com.yuAiTang.moxa.activity.template.EnhancedActivity;
import com.yuAiTang.moxa.entity.Equipment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class EquipmentSetting extends EnhancedActivity implements FragmentListener {
    private SQLiteDatabase db;
    LinkedList<Equipment> equips;
    private static final int[] EQUIP_IDS = new int[]{R.id.equip_1, R.id.equip_2, R.id.equip_3, R.id.equip_4,
            R.id.equip_5, R.id.equip_6, R.id.equip_7, R.id.equip_8};
    private final LinkedHashMap<Integer, DetailedEquipment> equipFrags = new LinkedHashMap<>();
    private static final String[] RELAY_NUMS = new String[]{"", "1", "2", "3", "4", "5", "6", "7", "8"};
    private static final ArrayList<String> unselectedReplays = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.equip_setting);
        db = Connector.getDb(this);
        equips = Connector.getEquips(db);
        unselectedReplays.addAll(Arrays.asList(RELAY_NUMS));
        for(Equipment equip : equips) unselectedReplays.remove(String.valueOf(equip.getRelay()));

        findViewById(R.id.back).setOnClickListener(v->{
            startActivity(new Intent(this, Manager.class));
            this.finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshEquipBox();
    }

    private void deleteEquip(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for(int id : EQUIP_IDS){
            FrameLayout view = findViewById(id);
            view.removeAllViewsInLayout();
        }
        for(DetailedEquipment frag : equipFrags.values()){
            transaction.remove(frag);
        }
        transaction.commit();
        equips = Connector.getEquips(db);
        unselectedReplays.add(" ");
        unselectedReplays.addAll(Arrays.asList(RELAY_NUMS));
        for(Equipment equip : equips) unselectedReplays.remove(String.valueOf(equip.getRelay()));

    }

    private void refreshEquipBox(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for(int id = 0; id < equips.size(); id++) {
            DetailedEquipment frag = DetailedEquipment.newInstance(this.equips.get(id), unselectedReplays.toArray(new String[]{}));
            transaction.add(EQUIP_IDS[id], frag, String.valueOf(EQUIP_IDS[id]));
            equipFrags.put(equips.get(id).getId(), frag);
        }
        if(equips.size() < 8) {
            DetailedEquipment frag = DetailedEquipment.newInstance();
            transaction.add(EQUIP_IDS[equips.size()], frag, "create");
            equipFrags.put(-1, frag);
        }
        transaction.commit();
    }

    @Override
    public void sendValue(String signal, Object value) {
        if(signal.equals("delete")){
            Connector.deleteEquip(db, (int) value);
        }else if(signal.equals("create")){
            // get a new relay
            Connector.insertEquip(db, new Equipment(equips.size() + 1, -1,  2, 0));
        }else if (signal.equals("update")){
            Equipment equip = (Equipment) value;
            Connector.updateEquip(db, equip.getType(), equip.getRelay(), equip.getId());
        }else if(signal.equals("select:relay")){
            if((int)value != 0){
            }
            return;
        }
        deleteEquip();
        refreshEquipBox();
    }
}
