package com.yuAiTang.moxa.activity.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.entity.Equipment;
import org.jetbrains.annotations.NotNull;

public class DetailedEquipment extends Fragment {

    private static final int[] EQUIP_STATUSES = new int[]{R.drawable.occupied, R.drawable.appointed, R.drawable.available};
    private static final int[] EQUIP_TYPE = new int[]{R.drawable.equip_1, R.drawable.equip_2, R.drawable.equip_3};
    private static final String[] EQUIP_TYPE_NAME = new String[]{"艾灸椅", "艾灸舱", "艾灸床 "};
    private static final String[] EQUIP_TEXT = new String[]{"使用中", "已预约", "空闲中"};

    private TextView header, delete, save, quit, equip_status;
    private ImageView create, typeIndicator, statusIndicator;
    private GridLayout detailedInfo;
    private LinearLayout rootView;
    private Spinner equip_type_spinner, relay_spinner;
    private Equipment equipment;
    private FragStatus fragStatus;

    public enum FragStatus {
        Normal, Update, Create
    }

    public void setFragStatus(FragStatus status) {
        switch (status) {
            case Normal:
                showNormal();
                break;
            case Update:
                showUpdate();
                break;
            case Create:
                showCreate();
                break;
        }
        fragStatus = status;
    }

    public void setRelaySpinner(String[] relays) {
        relay_spinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item, relays));
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.detailed_equip, container, false);
        header = root.findViewById(R.id.header);
        delete = root.findViewById(R.id.delete);
        save = root.findViewById(R.id.save);
        quit = root.findViewById(R.id.quit);
        create = root.findViewById(R.id.create);
        detailedInfo = root.findViewById(R.id.detailedInfo);
        statusIndicator = root.findViewById(R.id.statusIndicator);
        typeIndicator = root.findViewById(R.id.typeIndicator);
        equip_status = root.findViewById(R.id.equip_status);
        rootView = root.findViewById(R.id.root);
        equip_type_spinner = root.findViewById(R.id.equip_type);
        relay_spinner = root.findViewById(R.id.relay);
        equip_type_spinner.setAdapter(new ArrayAdapter<>(root.getContext(), R.layout.spinner_item, EQUIP_TYPE_NAME));
        equip_type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!EQUIP_TYPE_NAME[equipment.getType()].equals(equip_type_spinner.getSelectedItem()) || !String.valueOf(equipment.getRelay()).equals(relay_spinner.getSelectedItem())) {
                    setFragStatus(FragStatus.Update);
                    typeIndicator.setImageResource(EQUIP_TYPE[(int) id]);
                } else {
                    setFragStatus(FragStatus.Normal);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if (args != null) {
            equipment = (Equipment) args.getSerializable("equipment");
            if (equipment != null) {
                setFragStatus(FragStatus.Normal);
                statusIndicator.setImageResource(EQUIP_STATUSES[equipment.getStatus()]);
                typeIndicator.setImageResource(EQUIP_TYPE[equipment.getType()]);
                header.setText(String.format(getResources().getString(R.string.equipment), equipment.getId()));
                equip_status.setText(EQUIP_TEXT[equipment.getStatus()]);
                equip_type_spinner.setSelection(equipment.getType());
                String[] raw = args.getStringArray("unselectedRelays");
                String[] unselectedRelays = new String[raw.length + 1];
                unselectedRelays[0] = String.valueOf(equipment.getRelay());
                System.arraycopy(raw, 0, unselectedRelays, 1, raw.length);
                relay_spinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item, unselectedRelays));
            } else {
                setFragStatus(FragStatus.Create);
                header.setText("添加终端");
            }
        }
        if (getActivity() instanceof FragmentListener) {
            FragmentListener listener = (FragmentListener) getActivity();
            if (fragStatus == FragStatus.Create) {
                rootView.setOnClickListener(v -> listener.sendValue("create", ""));
            }
            delete.setOnClickListener(v -> {
                if (fragStatus == FragStatus.Normal && equipment != null)
                    listener.sendValue("delete", equipment.getId());
            });
            save.setOnClickListener(v -> {
                if (fragStatus == FragStatus.Update) {
                    String selectedItem = (String) equip_type_spinner.getSelectedItem();
                    int type = 0;
                    for (int i = 0; i < EQUIP_TYPE_NAME.length; i++) {
                        if (selectedItem.equals(EQUIP_TYPE_NAME[i])) {
                            type = i;
                        }
                    }
                    equipment.setType(type);
                    listener.sendValue("update", equipment);
                }
            });
            quit.setOnClickListener(v -> {
                if (fragStatus == FragStatus.Update) listener.sendValue("", "");
            });
            relay_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if(position != 0 || !EQUIP_TYPE_NAME[equipment.getType()].equals(equip_type_spinner.getSelectedItem())) {
                        listener.sendValue("select:relay", position);
                        setFragStatus(FragStatus.Update);
                    }else{
                        setFragStatus(FragStatus.Normal);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    private void showNormal() {
        detailedInfo.setVisibility(View.VISIBLE);
        delete.setVisibility(View.VISIBLE);
        create.setVisibility(View.GONE);
        save.setVisibility(View.GONE);
        quit.setVisibility(View.GONE);
    }

    private void showUpdate() {
        detailedInfo.setVisibility(View.VISIBLE);
        save.setVisibility(View.VISIBLE);
        quit.setVisibility(View.VISIBLE);
        create.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
    }

    private void showCreate() {
        create.setVisibility(View.VISIBLE);
        detailedInfo.setVisibility(View.GONE);
        quit.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
    }

    public static DetailedEquipment newInstance() {
        DetailedEquipment frag = new DetailedEquipment();
        Bundle args = new Bundle();
        args.putString("frag_status", FragStatus.Create.name());
        frag.setArguments(args);
        return frag;
    }

    public static DetailedEquipment newInstance(Equipment equip, String[] unselectedReplays) {
        DetailedEquipment frag = new DetailedEquipment();
        Bundle args = new Bundle();
        args.putSerializable("equipment", equip);
        args.putStringArray("unselectedRelays", unselectedReplays);
        frag.setArguments(args);
        return frag;

    }
}
