package com.bot.materialmaker.controller;

import com.bot.materialmaker.common.TaskPhases;
import com.bot.materialmaker.config.SimulationProperties;
import com.bot.materialmaker.dto.SimulationUpdate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示专用接口：运行时调整模拟耗时与失败注入阶段。
 * 仅供演示与联调使用，正式页面不依赖此接口。
 */
@RestController
@RequestMapping("/api/dev/simulation")
public class DevSimulationController {

    private final SimulationProperties simulation;

    public DevSimulationController(SimulationProperties simulation) {
        this.simulation = simulation;
    }

    @GetMapping
    public Map<String, Object> current() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("failPhase", simulation.getFailPhase().isEmpty() ? null : simulation.getFailPhase());
        body.put("stepDelayMillis", simulation.getStepDelayMillis());
        return body;
    }

    @PostMapping
    public Map<String, Object> update(@RequestBody SimulationUpdate update) {
        if (update.getFailPhase() != null) {
            String phase = update.getFailPhase().trim();
            if (!phase.isEmpty() && !TaskPhases.ALL.contains(phase)) {
                throw new IllegalArgumentException(
                        "无效阶段：" + phase + "，可选 " + TaskPhases.ALL);
            }
            simulation.setFailPhase(phase);
        }
        if (update.getStepDelayMillis() != null) {
            simulation.getStepDelayMillis().putAll(update.getStepDelayMillis());
        }
        return current();
    }
}
