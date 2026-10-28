package com.bot.materialmaker.model;

import java.time.LocalDateTime;
import java.util.List;

/** 模板。acceptedMimeTypes 在数据库中以逗号分隔存储，读取后拆为 List。 */
public class Template {

    private String id;
    private String title;
    private String category;
    private String coverUrl;
    private String description;
    private Integer requiredImageCount;
    private String acceptedMimeTypes;
    private Long maxFileSizeBytes;
    private String resultAsset;
    private LocalDateTime createdAt;

    public List<String> acceptedMimeList() {
        return List.of(acceptedMimeTypes.split(","));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRequiredImageCount() {
        return requiredImageCount;
    }

    public void setRequiredImageCount(Integer requiredImageCount) {
        this.requiredImageCount = requiredImageCount;
    }

    public String getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(String acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String getResultAsset() {
        return resultAsset;
    }

    public void setResultAsset(String resultAsset) {
        this.resultAsset = resultAsset;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
