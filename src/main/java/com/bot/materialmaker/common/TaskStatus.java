package com.bot.materialmaker.common;

import java.util.List;

/** 任务整体状态常量。 */
public final class TaskStatus {

    public static final String WAITING = "WAITING";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    public static final List<String> FINISHED = List.of(SUCCESS, FAILED);

    private TaskStatus() {
    }

    public static boolean isFinished(String status) {
        return FINISHED.contains(status);
    }
}
