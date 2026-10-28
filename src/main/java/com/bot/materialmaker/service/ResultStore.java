package com.bot.materialmaker.service;

import com.bot.materialmaker.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 结果整理：从 classpath 的演示资源中取出模板对应的结果图，
 * 复制到结果目录并以任务 ID 命名，使结果可通过 /files/results/** 访问。
 */
@Service
public class ResultStore {

    private final StorageProperties storageProperties;

    public ResultStore(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public String buildResult(String templateId, String asset, String taskId) throws IOException {
        ClassPathResource resource = new ClassPathResource("demo/results/" + asset);
        if (!resource.exists()) {
            throw new IOException("模板结果资源缺失：" + asset);
        }
        Path dir = storageProperties.resultDir();
        Files.createDirectories(dir);
        Path target = dir.resolve(taskId + ".png");
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/files/results/" + target.getFileName();
    }
}
