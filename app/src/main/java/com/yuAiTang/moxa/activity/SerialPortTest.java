package com.yuAiTang.moxa.activity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.activity.fragment.ConsoleFragment;
import com.yuAiTang.moxa.activity.template.ConsoleActivity;
import com.yuAiTang.moxa.activity.util.CameraUtil;
import com.yuAiTang.moxa.entity.Commands;
import com.yuAiTang.moxa.entity.Equipment;
import com.yuAiTang.moxa.util.Utils;

import java.util.*;

public class SerialPortTest extends ConsoleActivity {

    private static final String[] LOCATIONS = new String[]{"/dev/ttyS1", "/dev/ttyS3", "/dev/ttyS4"};
    // S1端口控制红外测温模块，S3端口控制艾灸设备使用状态（只有它是115200），S4端口控制设备开启关闭
    private static final Integer[] BAUDS = new Integer[]{9600, 115200};

    private Spinner port_location, baud;
    private final HashMap<Integer, TextView> equipViews = new HashMap<>(); // Key:设备ID, Value:对应的TextView
    private LinkedList<Equipment> equipments = new LinkedList<>();
    private TextView focusedEquipText;

    private LinearLayout occupied, appointed, available, shutdown, on, next;
    private CameraUtil cameraUtil;
    private int currentType = 0; // 0 S1红外模块， 1 S3端口， 2 S4端口， -1 搭配错误
    private ConsoleFragment consoleFrag;
    private SQLiteDatabase db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_port_test);
        port_location = findViewById(R.id.port_location);
        baud = findViewById(R.id.baud);
        focusedEquipText = findViewById(R.id.focusedEquipText);
        occupied = findViewById(R.id.occupied);
        appointed = findViewById(R.id.appointed);
        available = findViewById(R.id.available);
        shutdown = findViewById(R.id.shutdown);
        on = findViewById(R.id.on);
        next = findViewById(R.id.next);
        port_location.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, LOCATIONS));
        port_location.setOnItemSelectedListener(adapterListener);
        baud.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, BAUDS));
        baud.setOnItemSelectedListener(adapterListener);
        findViewById(R.id.restore).setOnClickListener(v->Toast.makeText(this, "功能开发中...", Toast.LENGTH_SHORT).show());
        // 返回按钮
        findViewById(R.id.back).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), Manager.class));
            this.finish();
        });
        occupied.setOnClickListener(listener);
        appointed.setOnClickListener(listener);
        available.setOnClickListener(listener);
        findViewById(R.id.clear).setOnClickListener(v -> {
            currentType = determineType();
            Intent intent = null;
            if (currentType != -1) {
                if (currentType == 0) {
                    intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s1");
                    intent.putExtra("clearPool", "true");
                } else if (currentType == 1) {
                    intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s3");
                    intent.putExtra("clearPool", "true");
                } else if (currentType == 2) {
                    intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s4");
                    intent.putExtra("clearPool", "true");
                }
                if (intent != null) sendBroadcast(intent);
            }
        });
        next.setOnClickListener(v->{
            Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s3");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("AA 97 00 03 55");
            String raw = Integer.toHexString((Integer.parseInt(focusedEquipText.getText().toString().split("#")[1]) - 1) * 8 + 1).toUpperCase(Locale.ROOT);
            raw = raw.length() == 1 ? "0" + raw : raw;
            commands.add("AA 98 " + raw + " 00 55");
            intent.putExtra("commands", commands);
            sendBroadcast(intent);
        });
        shutdown.setOnClickListener(listener);
        on.setOnClickListener(listener);
        cameraUtil = new CameraUtil("0", findViewById(R.id.prototype));
    }

    private final View.OnClickListener listener = v -> {
        if (equipments.size() > 0) {
            activeBtn((LinearLayout) v);
            int focusedId = Integer.parseInt(focusedEquipText.getText().toString().split("#")[1]);
            System.out.println(focusedId);
            if (v.getId() == R.id.occupied)
                Equipment.changeStatus(db, this, focusedId, 2);
            else if (v.getId() == R.id.appointed)
                Equipment.changeStatus(db, this, focusedId, 1);
            else if (v.getId() == R.id.available)
                Equipment.changeStatus(db, this, focusedId, 0);
            else if (v.getId() == R.id.on)
                Equipment.startup(this, focusedId, true);
            else if (v.getId() == R.id.shutdown)
                Equipment.startup(this, focusedId, false);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        consoleFrag = ConsoleFragment.newInstance();
        transaction.add(R.id.console, consoleFrag, "console");
        setConsole(consoleFrag);
        setReceiver(new ReqReceiver(), new RespReceiver());
        transaction.commit();
        db = Connector.getDb(this);
        equipments = Connector.getEquips(db);
        appendEquip(equipments);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            cameraUtil.stopPreview();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final AdapterView.OnItemSelectedListener adapterListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            currentType = determineType();
            if (currentType == 0) { // S1 摄像头模组
                findViewById(R.id.cameraRelevant).setVisibility(View.VISIBLE);
                findViewById(R.id.equipRelevant).setVisibility(View.GONE);
                unfocusEquips();
                if (cameraUtil != null) {
                    try {
                        cameraUtil.startPreview(getApplicationContext());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        Log.d("SerialPortTest", "摄像头未连接");
                        Toast.makeText(getApplicationContext(), "摄像头未连接", Toast.LENGTH_LONG).show();
                    }
                }
                consoleFrag.setAdapters(Commands.S1_COMMAND, Commands.S1_PARAM, Commands.S1_PARAM_2);
                consoleFrag.setEnterListener(v -> {
                    String[] input = consoleFrag.getInputCommand();
                    String[] command = consoleFrag.appendVerifiedSum(input);
                    Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s1");
                    intent.putExtra("command", command);
                    getApplicationContext().sendBroadcast(intent);
                    consoleFrag.setLastCommandType("s1:" + command[2]);
                });
            } else if (currentType == 1 || currentType == 2) { // S3 OR S4
                findViewById(R.id.cameraRelevant).setVisibility(View.GONE);
                findViewById(R.id.equipRelevant).setVisibility(View.VISIBLE);
                findViewById(R.id.focusedEquipText).setVisibility(View.GONE);
                findViewById(R.id.com_1_btnBox).setVisibility(View.GONE);
                findViewById(R.id.com_2_btnBox).setVisibility(View.GONE);
                unfocusEquips();
                if (cameraUtil != null) {
                    try {
                        cameraUtil.stopPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (currentType == 1) {
                    consoleFrag.setAdapters(Commands.S3_COMMAND, Commands.S3_PARAM, Commands.S3_PARAM_2);
                    consoleFrag.setEnterListener(v -> {
                        String[] input = consoleFrag.getInputCommand();
                        String[] command = new String[input.length + 1];
                        command[input.length] = "55";
                        System.arraycopy(input, 0, command, 0, input.length);
                        Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s3");
                        intent.putExtra("command", command);
                        getApplicationContext().sendBroadcast(intent);
                        consoleFrag.setLastCommandType("s2:" + command[2]);
                    });
                } else {
                    consoleFrag.setAdapters(Commands.S4_COMMAND, Commands.S4_PARAM, Commands.S4_PARAM_2);
                    consoleFrag.setEnterListener(v -> {
                        String[] input = consoleFrag.getInputCommand();
                        String[] command = new String[input.length + 3];
                        int sum = 0;
                        for (int i = 0; i < 12; i++) {
                            sum += Integer.parseInt(input[i], 16);
                        }
                        String raw = Integer.toHexString(sum).toUpperCase(Locale.ROOT);
                        command[input.length] = raw.length() <= 2 ? "0" + raw : raw.substring(raw.length() - 2);
                        command[input.length + 1] = "45";
                        command[input.length + 2] = "44";
                        System.arraycopy(input, 0, command, 0, input.length);
                        Intent intent = new Intent("com.yuAiTang.moxa.SerialPortUnit.s4");
                        intent.putExtra("command", command);
                        getApplicationContext().sendBroadcast(intent);
                        consoleFrag.setLastCommandType("S4-unique");
                    });
                }
            } else { // 完全错误的配对
                findViewById(R.id.cameraRelevant).setVisibility(View.GONE);
                findViewById(R.id.equipRelevant).setVisibility(View.GONE);
                findViewById(R.id.focusedEquipText).setVisibility(View.GONE);
                findViewById(R.id.com_1_btnBox).setVisibility(View.GONE);
                findViewById(R.id.com_2_btnBox).setVisibility(View.GONE);
                unfocusEquips();
                if (cameraUtil != null) {
                    try {
                        cameraUtil.stopPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                consoleFrag.setAdapters(new String[]{}, new String[]{}, new String[]{});
                consoleFrag.setEnterListener(v -> {
                    Toast.makeText(getApplicationContext(), "串口设置错误", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    /**
     * 根据从数据库获取到的设备信息集合，生成TextView并添加到页面里
     *
     * @param equips 设备信息集合
     * @see Equipment
     */
    private void appendEquip(@org.jetbrains.annotations.NotNull LinkedList<Equipment> equips) {
        if (equips.size() > 0) {
            for (Equipment equip : equips) {
                TextView equipView = new TextView(this);
                equipView.setText(String.format(getResources().getString(R.string.equipment), equip.getId()));
                equipView.setGravity(Gravity.CENTER);
                equipView.setBackgroundResource(R.drawable.border);
                equipView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                equipView.setTextSize(getResources().getDimension(R.dimen.regularFontSize));
                equipView.setWidth(Utils.dip2px(this, 135));
                equipView.setHeight(Utils.dip2px(this, 80));
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                int margin = Utils.dip2px(this, 20);
                params.setMargins(margin, 0, margin, margin);
                equipView.setLayoutParams(params);
                equipView.setOnClickListener(v -> focusEquip(equip));
                ((GridLayout) findViewById(R.id.equipments)).addView(equipView);
                equipViews.put(equip.getId(), equipView);
            }
            focusEquip(equips.getFirst());
        }
    }

    /**
     * 设备列表TextView点击事件，将展示被点击设备的信息
     *
     * @param equip 设备对象
     */
    private void focusEquip(Equipment equip) {
        int equipId = equip.getId();
        unfocusEquips();
        Objects.requireNonNull(equipViews.get(equipId)).setTextColor(ContextCompat.getColor(this, R.color.white));
        Objects.requireNonNull(equipViews.get(equipId)).setBackgroundResource(R.drawable.color_primary_background);
        focusedEquipText.setText(String.format(getResources().getString(R.string.equipment), equipId));
        findViewById(R.id.focusedEquipText).setVisibility(View.VISIBLE);
        if (currentType == 1) {
            findViewById(R.id.com_1_btnBox).setVisibility(View.VISIBLE);
            switch (equip.getStatus()) {
                case 0:
                    activeBtn(occupied);
                    break;
                case 1:
                    activeBtn(appointed);
                    break;
                case 2:
                    activeBtn(available);
                    break;
                case 4:
                    activeBtn(shutdown);
                    break;
                case 5:
                    activeBtn(on);
                    break;
            }
        } else if (currentType == 2) {
            findViewById(R.id.com_2_btnBox).setVisibility(View.VISIBLE);
            activeBtn(shutdown); // TODO 暂且默认设备都是开机的
        }
    }

    /**
     * 将被点击设备的样式改变全部重置
     */
    public void unfocusEquips() {
        for (TextView view : equipViews.values()) {
            view.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            view.setBackgroundResource(R.drawable.border);
        }
    }

    /**
     * 将样式改为启用样式
     * 0 使用中, 1 已预约, 2空闲中, 3 呼叫, 4 关机, 5开机
     */
    private void activeBtn(LinearLayout layout) {
        inactiveAllExceptCall();
        layout.setBackgroundResource(R.drawable.color_primary_background);
        ((TextView) layout.getChildAt(1)).setTextColor(ContextCompat.getColor(this, R.color.white));

    }

    /**
     * 将样式改为未启用样式
     */
    private void inactiveBtn(LinearLayout layout) {
        layout.setBackgroundResource(R.drawable.border);
        ((TextView) layout.getChildAt(1)).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

    }

    private void inactiveAllExceptCall() {
        if (occupied != null) inactiveBtn(occupied);
        if (appointed != null) inactiveBtn(appointed);
        if (available != null) inactiveBtn(available);
        if (shutdown != null) inactiveBtn(shutdown);
        if (on != null) inactiveBtn(on);
    }

    private int determineType() {
        String selectedPort = (String) port_location.getSelectedItem();
        int selectedBaud = (int) baud.getSelectedItem();
        if (selectedPort.equals(LOCATIONS[0]) && selectedBaud == BAUDS[1]) {
            return 0;
        } else if (selectedPort.equals(LOCATIONS[1]) && selectedBaud == BAUDS[1]) {
            return 1;
        } else if (selectedPort.equals(LOCATIONS[2]) && selectedBaud == BAUDS[0]) {
            return 2;
        }
        return -1;
    }
}
