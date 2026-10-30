package com.bot.materialmaker.common;

import java.util.List;

/** 任务当前阶段常量（后台流水线的三个步骤）。 */
public final class TaskPhases {

    public static final String MATERIAL_CHECK = "MATERIAL_CHECK";
    public static final String GENERATING = "GENERATING";
    public static final String ASSEMBLING = "ASSEMBLING";

    public static final List<String> ALL = List.of(MATERIAL_CHECK, GENERATING, ASSEMBLING);

    public static final String DISPLAY_MATERIAL_CHECK = "素材检查";
    public static final String DISPLAY_GENERATING = "模拟生成";
    public static final String DISPLAY_ASSEMBLING = "结果整理";

    private TaskPhases() {
    }

    public static String display(String phase) {
        if (phase == null) {
            return null;
        }
        return switch (phase) {
            case MATERIAL_CHECK -> DISPLAY_MATERIAL_CHECK;
            case GENERATING -> DISPLAY_GENERATING;
            case ASSEMBLING -> DISPLAY_ASSEMBLING;
            default -> phase;
        };
    }
}
