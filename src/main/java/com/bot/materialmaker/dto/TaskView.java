package com.bot.materialmaker.dto;

import java.util.List;

public class TaskView {

    private String id;
    private String templateId;
    private String templateTitle;
    private String status;
    private String currentPhase;
    private String currentPhaseDisplay;
    private String failPhase;
    private String failReason;
    private String resultUrl;
    private String createdAt;
    private String updatedAt;
    private String finishedAt;
    private List<TaskStepView> steps;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateTitle() {
        return templateTitle;
    }

    public void setTemplateTitle(String templateTitle) {
        this.templateTitle = templateTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public String getCurrentPhaseDisplay() {
        return currentPhaseDisplay;
    }

    public void setCurrentPhaseDisplay(String currentPhaseDisplay) {
        this.currentPhaseDisplay = currentPhaseDisplay;
    }

    public String getFailPhase() {
        return failPhase;
    }

    public void setFailPhase(String failPhase) {
        this.failPhase = failPhase;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<TaskStepView> getSteps() {
        return steps;
    }

    public void setSteps(List<TaskStepView> steps) {
        this.steps = steps;
    }
}
