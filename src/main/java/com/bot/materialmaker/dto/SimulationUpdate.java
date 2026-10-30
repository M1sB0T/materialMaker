package com.bot.materialmaker.dto;

import java.util.Map;

/** 开发/演示接口请求体：运行时调整模拟耗时与失败注入。 */
public class SimulationUpdate {

    /** 注入失败的阶段：MATERIAL_CHECK / GENERATING / ASSEMBLING / 空串（不失败） */
    private String failPhase;
    /** 整体替换每阶段耗时配置（毫秒），为 null 时保持不变 */
    private Map<String, Long> stepDelayMillis;

    public String getFailPhase() {
        return failPhase;
    }

    public void setFailPhase(String failPhase) {
        this.failPhase = failPhase;
    }

    public Map<String, Long> getStepDelayMillis() {
        return stepDelayMillis;
    }

    public void setStepDelayMillis(Map<String, Long> stepDelayMillis) {
        this.stepDelayMillis = stepDelayMillis;
    }
}
