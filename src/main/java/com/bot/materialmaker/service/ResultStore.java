package com.bot.materialmaker.service;

import com.bot.materialmaker.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * 结果整理：模拟生成的输出图来自一个随机池子——
 * 模板自带的色块结果图 + demo/results/pool/ 下的真实示例图。
 * 每次任务随机取一张，复制到结果目录并以任务 ID 命名，
 * 通过 /files/results/** 对外预览。
 */
@Service
public class ResultStore {

    private static final String POOL_PATTERN = "classpath*:demo/results/pool/*";

    private final StorageProperties storageProperties;
    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    public ResultStore(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public String buildResult(String templateId, String asset, String taskId) throws IOException {
        List<Resource> candidates = new ArrayList<>();
        // 池子 1：模板对应的色块结果图
        candidates.add(new ClassPathResource("demo/results/" + asset));
        // 池子 2：真实示例图（demo/results/pool/ 下所有文件）
        try {
            for (Resource r : resolver.getResources(POOL_PATTERN)) {
                if (r.exists() && r.isReadable()) {
                    candidates.add(r);
                }
            }
        } catch (IOException e) {
            // 池子扫描失败不阻断任务，仅回退到模板自带结果图
            log("示例图池扫描失败，回退模板结果图：{}", e.getMessage());
        }
        if (candidates.isEmpty()) {
            throw new IOException("结果图池为空，模板结果资源缺失：" + asset);
        }

        Resource chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        String sourceName = chosen.getFilename();
        String ext = extensionOf(sourceName);

        Path dir = storageProperties.resultDir();
        Files.createDirectories(dir);
        Path target = dir.resolve(taskId + ext);
        try (InputStream in = chosen.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log("任务 {} 从 {} 张候选图中随机选中 {}", taskId, candidates.size(), sourceName);
        return "/files/results/" + target.getFileName();
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return ".png";
        }
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot) : ".png";
        // 仅放行图片扩展名，避免异常文件名拼进结果路径
        return ext.matches("(?i)\\.(png|jpe?g|webp|gif|bmp)") ? ext.toLowerCase() : ".png";
    }

    private void log(String format, Object... args) {
        org.slf4j.LoggerFactory.getLogger(ResultStore.class).info(format, args);
    }
}
