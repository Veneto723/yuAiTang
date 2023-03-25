package com.yuAiTang.moxa.entity;

import java.io.Serializable;

public class Resource implements Serializable {
    private int id;
    private String filename;
    private String path;
    private int type;

    /**
     * Basic constructor
     * @param id ID 文件排序（播放顺序，越小越先播放）
     * @param filename 文件名（去后缀）
     * @param path 文件本地路径
     * @param type 资源类型 0 视频， 1 照片
     */
    public Resource(int id, String filename, String path, int type) {
        this.id = id;
        this.filename = filename;
        this.path = path;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String[] stringify(){
        return new String[]{String.valueOf(id), String.valueOf(filename), String.valueOf(path), String.valueOf(type)};
    }


    public static boolean isVideo(String path){
        String type = path.substring(path.lastIndexOf(".") + 1);
        return type.equals("avi") || type.equals("mp4");
    }
}
