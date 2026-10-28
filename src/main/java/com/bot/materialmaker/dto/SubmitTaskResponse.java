package com.bot.materialmaker.dto;

/** 任务受理响应：仅表示任务已创建并排队，不代表制作完成。 */
public class SubmitTaskResponse {

    private String taskId;
    private String status;

    public SubmitTaskResponse(String taskId, String status) {
        this.taskId = taskId;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getStatus() {
        return status;
    }
}
