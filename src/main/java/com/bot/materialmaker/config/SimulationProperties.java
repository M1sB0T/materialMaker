package com.bot.materialmaker.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模拟生成器的可调配置：
 * - stepDelayMillis：每个阶段模拟耗时（毫秒）
 * - failPhase：注入失败的阶段（MATERIAL_CHECK / GENERATING / ASSEMBLING，空串表示不失败）
 * 可通过 /api/dev/simulation 接口在运行时修改，便于演示。
 */
@Component
@ConfigurationProperties(prefix = "simulation")
public class SimulationProperties {

    /** 后台流水线线程读取、dev 接口线程写入，必须使用并发安全实现 */
    private Map<String, Long> stepDelayMillis = new ConcurrentHashMap<>();

    private String failPhase = "";

    public long delayFor(String phase) {
        return stepDelayMillis.getOrDefault(phase, 1500L);
    }

    public Map<String, Long> getStepDelayMillis() {
        return stepDelayMillis;
    }

    public void setStepDelayMillis(Map<String, Long> stepDelayMillis) {
        this.stepDelayMillis = stepDelayMillis;
    }

    public String getFailPhase() {
        return failPhase;
    }

    public void setFailPhase(String failPhase) {
        this.failPhase = failPhase == null ? "" : failPhase.trim();
    }
}
