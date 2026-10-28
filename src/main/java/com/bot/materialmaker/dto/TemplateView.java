package com.bot.materialmaker.dto;

import java.util.List;

public class TemplateView {

    private String id;
    private String title;
    private String category;
    private String coverUrl;
    private String description;
    private Integer requiredImageCount;
    private List<String> acceptedMimeTypes;
    private Long maxFileSizeBytes;

    public TemplateView(String id, String title, String category, String coverUrl, String description,
                        Integer requiredImageCount, List<String> acceptedMimeTypes, Long maxFileSizeBytes) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.coverUrl = coverUrl;
        this.description = description;
        this.requiredImageCount = requiredImageCount;
        this.acceptedMimeTypes = acceptedMimeTypes;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public Integer getRequiredImageCount() {
        return requiredImageCount;
    }

    public List<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }
}
