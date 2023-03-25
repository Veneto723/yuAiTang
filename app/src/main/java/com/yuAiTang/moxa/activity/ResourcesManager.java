package com.yuAiTang.moxa.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.yuAiTang.moxa.Db.Connector;
import com.yuAiTang.moxa.R;
import com.yuAiTang.moxa.activity.template.EnhancedActivity;
import com.yuAiTang.moxa.activity.util.Controller;
import com.yuAiTang.moxa.entity.Resource;
import com.yuAiTang.moxa.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ResourcesManager extends EnhancedActivity {

    private SQLiteDatabase db;
    private LinkedHashMap<Integer, Resource> resources;
    private final LinkedHashMap<Integer, TextView> resourceViews = new LinkedHashMap<>();
    private TextView upward;
    private TextView downward;
    private TextView filename;
    private TextView path;
    private int focusedID = 1;
    private VideoView videoView;
    private ImageView imageView;

    private void disableBtn(TextView view) {
        view.setEnabled(false);
        view.setBackgroundResource(R.drawable.grey_background);
        view.setTextColor(ContextCompat.getColor(this, R.color.black));
    }

    private void enableBtn(TextView view){
        view.setEnabled(true);
        view.setBackgroundResource(R.drawable.border);
        view.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
    }

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resources_manager);
        upward = findViewById(R.id.upward);
        downward = findViewById(R.id.downward);
        TextView delete = findViewById(R.id.delete);
        filename = findViewById(R.id.filename);
        path = findViewById(R.id.path);
        videoView = findViewById(R.id.videoView);
        videoView.setMediaController(new MediaController(this));
        imageView = findViewById(R.id.imageView);
        TextView addResource = findViewById(R.id.addResource);
        findViewById(R.id.back).setOnClickListener(v->{
            startActivity(new Intent(this, Manager.class));
            this.finish();
        });
        delete.setOnClickListener(v->{
            resources = Connector.deleteResource(db, focusedID);
            focusedID = 1;
            rearrangeResources();
        });
        upward.setOnClickListener(v->{
            Connector.upwardResource(db, focusedID);
            focusedID --;
            rearrangeResources();
            if(focusedID == 1){
                disableBtn(upward);
            }else{
                enableBtn(upward);
                enableBtn(downward);
            }
        });
        downward.setOnClickListener(v->{
            Connector.downwardResource(db, focusedID);
            focusedID ++;
            rearrangeResources();
            if(focusedID == resources.size()){
                disableBtn(downward);
            }else{
                enableBtn(upward);
                enableBtn(downward);
            }
        });
        addResource.setOnClickListener(v->{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);//意图：文件浏览器
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"video/x-msvideo", "video/mp4", "image/jpeg", "image/png"});
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//关键！多选参数
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        db = Connector.getDb(this);
        resources = Connector.getResources(db);
        appendResource();
        focusResource(1);
    }

    private void refreshListTitle(){
        ((TextView) findViewById(R.id.resourceListText)).setText(String.format(getResources().getString(R.string.resources_list), resources.size()));
    }

    private void rearrangeResources(){
        resources = Connector.getResources(db);
        LinearLayout layout = findViewById(R.id.resourceList);
        layout.removeAllViews();
        appendResource();
        focusResource(focusedID);
    }


    private void appendResource(){
        refreshListTitle();
        LinearLayout layout = findViewById(R.id.resourceList);
        for(Resource resource : resources.values()){
            TextView view = new TextView(this);
            view.setText(resource.getFilename());
            view.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            view.setTextSize(getResources().getDimension(R.dimen.bigFontSize));
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setPadding(Utils.dip2px(this, 20), 0, 0, 0);
            view.setTag(resource.getId());
            view.setBackgroundResource(R.drawable.border);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.width = Utils.dip2px(this, 750);
            params.height = Utils.dip2px(this, 75);
            params.setMargins(0, 0, 0, Utils.dip2px(this, 20));
            view.setLayoutParams(params);
            view.setOnClickListener(listener);
            layout.addView(view);
            resourceViews.put(resource.getId(), view);
        }
    }

    View.OnClickListener listener = v->{
            TextView view = resourceViews.get(focusedID);
            view.setBackgroundResource(R.drawable.border);
            view.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        focusedID = (int) v.getTag();
        focusResource(focusedID);
    };

    private void focusResource(int focusedID){
        TextView view = resourceViews.get(focusedID);
        assert view != null;
        view.setBackgroundResource(R.drawable.color_primary_background);
        view.setTextColor(ContextCompat.getColor(this, R.color.white));
        Resource resource = resources.get(focusedID);
        assert resource != null;
        filename.setText(String.format(getResources().getString(R.string.filename), resource.getFilename()));
        String file_path = resource.getPath();
        path.setText(String.format(getResources().getString(R.string.path), file_path));

        if(resource.getType() == 0){// 视频
            videoView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            videoView.setVideoURI(Uri.parse(file_path));
            videoView.start();
        }else{ // 照片
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            Bitmap img = Controller.getBitmap(file_path);
            if(img == null) img = Controller.addBitmap(Utils.getLocalImg(file_path), file_path);
            imageView.setImageBitmap(img);
        }
        if(focusedID == 1){
            disableBtn(upward);
            enableBtn(downward);
        }else if(focusedID == resources.size()){
            disableBtn(downward);
            enableBtn(upward);
        }else{
            enableBtn(upward);
            enableBtn(downward);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getData() != null) {
                //单选文件
                Uri uri = data.getData();
                String path = Utils.getFilePathByUri(getApplicationContext(), uri);
                Connector.insertResource(db, path);
                rearrangeResources();
            } else {
                //多选文件
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    List<String> pathList = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();
                        String path = Utils.getFilePathByUri(getApplicationContext(), uri);
                        pathList.add(path);
                    }
                    Connector.insertResources(db, pathList);
                    rearrangeResources();
                }
            }
        }
    }
}
