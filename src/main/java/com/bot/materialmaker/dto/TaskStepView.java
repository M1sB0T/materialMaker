package com.bot.materialmaker.dto;

public class TaskStepView {

    private String step;
    private String stepDisplay;
    private String status;
    private String message;
    private String startedAt;
    private String finishedAt;

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStepDisplay() {
        return stepDisplay;
    }

    public void setStepDisplay(String stepDisplay) {
        this.stepDisplay = stepDisplay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }
}
