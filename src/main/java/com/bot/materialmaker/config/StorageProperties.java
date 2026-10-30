package com.bot.materialmaker.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地文件存储位置配置（上传素材与生成结果）。 */
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String root = "./data";

    public Path uploadDir() {
        return Path.of(root, "uploads").toAbsolutePath().normalize();
    }

    public Path resultDir() {
        return Path.of(root, "results").toAbsolutePath().normalize();
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
