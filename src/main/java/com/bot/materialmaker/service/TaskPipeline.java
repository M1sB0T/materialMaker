package com.bot.materialmaker.service;

import com.bot.materialmaker.common.TaskPhases;
import com.bot.materialmaker.common.TaskStatus;
import com.bot.materialmaker.config.SimulationProperties;
import com.bot.materialmaker.mapper.TaskMapper;
import com.bot.materialmaker.mapper.TaskStepMapper;
import com.bot.materialmaker.mapper.TemplateMapper;
import com.bot.materialmaker.model.Task;
import com.bot.materialmaker.model.Template;
import java.io.IOException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 后台三步流水线：素材检查 → 模拟生成 → 结果整理。
 * 在独立线程中执行，与 HTTP 请求解耦；每个阶段都有独立的
 * 数据库状态边界（task.current_phase + task_steps 记录），
 * 关闭页面不影响执行。
 */
@Service
public class TaskPipeline {

    private static final Logger log = LoggerFactory.getLogger(TaskPipeline.class);

    private final TaskMapper taskMapper;
    private final TaskStepMapper taskStepMapper;
    private final TemplateMapper templateMapper;
    private final SimulationProperties simulation;
    private final ResultStore resultStore;

    public TaskPipeline(TaskMapper taskMapper, TaskStepMapper taskStepMapper,
                        TemplateMapper templateMapper, SimulationProperties simulation,
                        ResultStore resultStore) {
        this.taskMapper = taskMapper;
        this.taskStepMapper = taskStepMapper;
        this.templateMapper = templateMapper;
        this.simulation = simulation;
        this.resultStore = resultStore;
    }

    public void run(String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            log.warn("任务 {} 不存在，跳过执行", taskId);
            return;
        }
        // 已受理过的任务不重复执行（防止重复投递）
        if (!TaskStatus.WAITING.equals(task.getStatus())) {
            log.info("任务 {} 状态为 {}，跳过重复执行", taskId, task.getStatus());
            return;
        }
        Template template = templateMapper.findById(task.getTemplateId());
        if (template == null) {
            markFailed(taskId, task.getCurrentPhase(), "模板不存在或已被移除，任务终止");
            return;
        }

        log.info("任务 {} 开始处理，模板 {}", taskId, template.getId());
        taskMapper.updateStatus(taskId, TaskStatus.PROCESSING);

        String resultUrl = null;
        for (String phase : TaskPhases.ALL) {
            taskMapper.updatePhase(taskId, phase);
            taskStepMapper.markRunning(taskId, phase, LocalDateTime.now());

            // 模拟该步骤的耗时
            if (!sleepQuietly(simulation.delayFor(phase))) {
                markFailed(taskId, phase, "任务线程被中断，任务终止");
                return;
            }

            // 失败注入：命中配置阶段则终止后续所有步骤
            if (phase.equals(simulation.getFailPhase())) {
                markFailed(taskId, phase,
                        "模拟失败：步骤[" + TaskPhases.display(phase) + "]被注入失败，后续步骤已停止");
                return;
            }

            // 结果整理：生成结果文件并记录可预览地址
            if (TaskPhases.ASSEMBLING.equals(phase)) {
                try {
                    resultUrl = resultStore.buildResult(template.getId(), template.getResultAsset(), taskId);
                } catch (IOException e) {
                    markFailed(taskId, phase, "结果整理失败：" + e.getMessage());
                    return;
                }
            }

            taskStepMapper.markSuccess(taskId, phase, LocalDateTime.now());
            log.info("任务 {} 完成阶段 {}", taskId, phase);
        }

        taskMapper.markSuccess(taskId, resultUrl, LocalDateTime.now());
        log.info("任务 {} 处理成功，结果 {}", taskId, resultUrl);
    }

    private void markFailed(String taskId, String failPhase, String reason) {
        log.warn("任务 {} 失败于阶段 {}：{}", taskId, failPhase, reason);
        taskStepMapper.markFailed(taskId, failPhase, LocalDateTime.now(), reason);
        taskMapper.markFailed(taskId, failPhase, reason, LocalDateTime.now());
    }

    private boolean sleepQuietly(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
