package com.bot.materialmaker.dto;

import java.util.List;

public class SubmitTaskRequest {

    private String templateId;
    private List<Long> materialIds;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public List<Long> getMaterialIds() {
        return materialIds;
    }

    public void setMaterialIds(List<Long> materialIds) {
        this.materialIds = materialIds;
    }
}
