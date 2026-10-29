package com.bot.materialmaker.controller;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.common.TaskPhases;
import com.bot.materialmaker.config.SimulationProperties;
import com.bot.materialmaker.dto.SimulationUpdate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示专用接口：运行时调整模拟耗时与失败注入阶段。
 * 仅供演示与联调使用，正式页面不依赖此接口。
 *
 * 安全要点：
 * - 通过 app.dev-tools.enabled 开关控制（默认关闭，关闭时对外表现为 404）；
 *   Docker 演示部署通过 DEV_TOOLS_ENABLED=true 打开。
 * - 阶段耗时限制在 0~600 秒，防止恶意注入超长耗时把流水线线程池占满（DoS）。
 */
@RestController
@RequestMapping("/api/dev/simulation")
public class DevSimulationController {

    /** 单阶段模拟耗时的安全上限（毫秒） */
    private static final long MAX_STEP_DELAY_MILLIS = 600_000L;

    private final SimulationProperties simulation;
    private final boolean enabled;

    public DevSimulationController(SimulationProperties simulation,
                                   @Value("${app.dev-tools.enabled:false}") boolean enabled) {
        this.simulation = simulation;
        this.enabled = enabled;
    }

    @GetMapping
    public Map<String, Object> current() {
        requireEnabled();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("failPhase", simulation.getFailPhase().isEmpty() ? null : simulation.getFailPhase());
        body.put("stepDelayMillis", simulation.getStepDelayMillis());
        return body;
    }

    @PostMapping
    public Map<String, Object> update(@RequestBody SimulationUpdate update) {
        requireEnabled();
        if (update.getFailPhase() != null) {
            String phase = update.getFailPhase().trim();
            if (!phase.isEmpty() && !TaskPhases.ALL.contains(phase)) {
                throw new IllegalArgumentException(
                        "无效阶段：" + phase + "，可选 " + TaskPhases.ALL);
            }
            simulation.setFailPhase(phase);
        }
        if (update.getStepDelayMillis() != null) {
            for (Map.Entry<String, Long> entry : update.getStepDelayMillis().entrySet()) {
                if (!TaskPhases.ALL.contains(entry.getKey())) {
                    throw new IllegalArgumentException(
                            "无效阶段：" + entry.getKey() + "，可选 " + TaskPhases.ALL);
                }
                long delay = entry.getValue() == null ? 0L : entry.getValue();
                simulation.getStepDelayMillis().put(entry.getKey(),
                        Math.max(0L, Math.min(delay, MAX_STEP_DELAY_MILLIS)));
            }
        }
        return current();
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "接口不存在");
        }
    }
}
