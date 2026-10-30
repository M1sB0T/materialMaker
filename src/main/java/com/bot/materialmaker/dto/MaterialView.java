package com.bot.materialmaker.dto;

public class MaterialView {

    private Long id;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private String previewUrl;

    public MaterialView(Long id, String originalName, String mimeType, Long sizeBytes, String previewUrl) {
        this.id = id;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.previewUrl = previewUrl;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }
}
