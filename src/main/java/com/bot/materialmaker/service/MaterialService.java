package com.bot.materialmaker.service;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.config.StorageProperties;
import com.bot.materialmaker.dto.MaterialView;
import com.bot.materialmaker.mapper.MaterialMapper;
import com.bot.materialmaker.mapper.TemplateMapper;
import com.bot.materialmaker.model.Material;
import com.bot.materialmaker.model.Template;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 素材上传：按模板要求做服务端校验（类型/大小/真实文件内容），
 * 文件保存到本地 uploads 目录并登记数据库记录。
 *
 * 安全要点：
 * - 存储扩展名只由服务端按 MIME 白名单映射得出，绝不采用客户端原始文件名，
 *   防止上传 evil.html / evil.svg 等伪装成图片的可执行内容（存储型 XSS）；
 * - 校验文件头魔数，确保内容确为声明格式的图片，防止伪造 Content-Type 绕过。
 */
@Service
public class MaterialService {

    /** 服务端允许的 MIME → 存储扩展名映射（与模板 accepted_mime_types 保持一致的子集） */
    private static final Map<String, String> MIME_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png");

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
        String extension = MIME_EXTENSION.get(mimeType);
        if (extension == null || !template.acceptedMimeList().contains(mimeType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "不支持的图片类型：" + mimeType + "，模板仅接受 " + template.getAcceptedMimeTypes());
        }
        if (file.getSize() > template.getMaxFileSizeBytes()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "图片超过大小限制（最大 " + template.getMaxFileSizeBytes() / 1024 / 1024 + "MB）");
        }
        // 魔数校验：Content-Type 是客户端自报的，必须核实文件内容与声明格式一致
        if (!matchesMagicNumber(file, mimeType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "文件内容不是有效的 " + mimeType + " 图片");
        }

        Path dir = storageProperties.uploadDir();
        try {
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + extension;
            Path target = dir.resolve(storedName);
            file.transferTo(target);
            Material material = new Material();
            material.setUserId(userId);
            material.setOriginalName(safeName(file.getOriginalFilename()));
            material.setStoredPath(target.toString());
            material.setMimeType(mimeType);
            material.setSizeBytes(file.getSize());
            try {
                materialMapper.insert(material);
            } catch (RuntimeException e) {
                // 落库失败时回滚已写入的文件，避免产生无数据库记录的孤儿文件
                Files.deleteIfExists(target);
                throw e;
            }
            return new MaterialView(material.getId(), material.getOriginalName(), mimeType,
                    file.getSize(), "/files/uploads/" + storedName);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "素材保存失败：" + e.getMessage());
        }
    }

    /** 校验文件头部魔数与声明的 MIME 一致（仅支持 JPEG / PNG） */
    private boolean matchesMagicNumber(MultipartFile file, String mimeType) {
        byte[] header = new byte[8];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, header.length);
            if (mimeType.equals("image/png")) {
                // PNG 固定 8 字节签名：89 50 4E 47 0D 0A 1A 0A
                return read == 8
                        && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E
                        && header[3] == 0x47;
            }
            if (mimeType.equals("image/jpeg")) {
                // JPEG 以 FF D8 FF 开头
                return read >= 3 && (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** 原始文件名仅用于展示，去除路径分隔与换行等控制字符 */
    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[\\r\\n\\\\/]", "_");
    }
}
