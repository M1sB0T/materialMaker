package com.bot.materialmaker.service;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.config.StorageProperties;
import com.bot.materialmaker.dto.MaterialView;
import com.bot.materialmaker.mapper.MaterialMapper;
import com.bot.materialmaker.mapper.TemplateMapper;
import com.bot.materialmaker.model.Material;
import com.bot.materialmaker.model.Template;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 素材上传：按模板要求做服务端校验（类型/大小），
 * 文件保存到本地 uploads 目录并登记数据库记录。
 */
@Service
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final TemplateMapper templateMapper;
    private final StorageProperties storageProperties;

    public MaterialService(MaterialMapper materialMapper, TemplateMapper templateMapper,
                           StorageProperties storageProperties) {
        this.materialMapper = materialMapper;
        this.templateMapper = templateMapper;
        this.storageProperties = storageProperties;
    }

    public MaterialView upload(Long userId, String templateId, MultipartFile file) {
        Template template = templateMapper.findById(templateId);
        if (template == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "模板不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }
        String mimeType = file.getContentType() == null ? "" : file.getContentType();
        if (!template.acceptedMimeList().contains(mimeType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "不支持的图片类型：" + mimeType + "，模板仅接受 " + template.getAcceptedMimeTypes());
        }
        if (file.getSize() > template.getMaxFileSizeBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "图片超过大小限制（最大 " + template.getMaxFileSizeBytes() / 1024 / 1024 + "MB）");
        }
        Path dir = storageProperties.uploadDir();
        try {
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + extensionOf(file.getOriginalFilename(), mimeType);
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            Material material = new Material();
            material.setUserId(userId);
            material.setOriginalName(safeName(file.getOriginalFilename()));
            material.setStoredPath(target.toString());
            material.setMimeType(mimeType);
            material.setSizeBytes(file.getSize());
            materialMapper.insert(material);
            return new MaterialView(material.getId(), material.getOriginalName(), mimeType,
                    file.getSize(), "/files/uploads/" + storedName);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "素材保存失败：" + e.getMessage());
        }
    }

    private String extensionOf(String originalName, String mimeType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.length() <= 6) {
                return ext;
            }
        }
        return mimeType.contains("png") ? ".png" : ".jpg";
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[\\r\\n\\\\/]", "_");
    }
}
