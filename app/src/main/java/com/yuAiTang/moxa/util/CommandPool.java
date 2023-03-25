package com.yuAiTang.moxa.util;

import android.content.Context;
import com.yuAiTang.moxa.entity.Tracks;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * 指令池，负责指令的分发和存储。遵循FIFO原则
 */
public abstract class CommandPool {

    private final LinkedList<String[]> pool;
    private final String[] uniqueList;
    private final Context context;
    private String lastCommandType;

    public CommandPool(Context context, String[] uniqueList) {
        this.pool = new LinkedList<>();
        this.context = context;
        this.lastCommandType = "";
        this.uniqueList = uniqueList;
    }

    public String getLastCommandType() {
        return lastCommandType;
    }

    public void setLastCommandType(String lastCommandType) {
        this.lastCommandType = lastCommandType;
    }

    /**
     * 获取指令池长度
     *
     * @return 指令池长度
     */
    public int size() {
        return pool.size();
    }

    /**
     * 获取指令池头部指令
     *
     * @return 指令
     */
    public String[] getFirst() {
        if (pool.size() == 0) return null;
        else return pool.get(0);
    }

    /**
     * 清空指令池
     */
    public void clear() {
        Tracker.track(context, Tracks.command_pool, "指令池清空");
        pool.clear();
    }

    /**
     * 追加单条指令
     *
     * @param command 指令
     */
    public void appendCommand(String[] command) {
        String commandType = command[0];
        if (uniqueList != null) {
            if (Arrays.asList(uniqueList).contains(commandType)) {
                for (String[] poolCommand : pool) {
                    if (poolCommand[0].equals(commandType)) {
                        return;
                    }
                }
            }
        }
        Tracker.track(context, Tracks.command_pool, "指令池追加指令" + Arrays.toString(command).toUpperCase());
        pool.add(command);
    }

    /**
     * 在指令池头部追加指令
     *
     * @param command 指令
     */
    public void prepend(String[] command) {
        String commandType = command[1];
        if (uniqueList != null) {
            if (Arrays.asList(uniqueList).contains(commandType)) {
                for (String[] poolCommand : pool) {
                    if (poolCommand[1].equals(commandType)) {
                        pool.remove(poolCommand);
                    }
                }
            }
        }
        Tracker.track(context, Tracks.command_pool, "指令池[头部]追加指令" + Arrays.toString(command).toUpperCase());
        pool.addFirst(command);
    }

    /**
     * 删除指令池头指令
     */
    public void shift() {
        if (pool.size() > 0) {
            Tracker.track(context, Tracks.command_pool, "指令池移除指令" + Arrays.toString(getFirst()).toUpperCase());
            pool.remove(0);
        }
    }

    public abstract void dispel(String[] receive);

}
