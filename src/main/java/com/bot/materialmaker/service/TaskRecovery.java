package com.bot.materialmaker.service;

import com.bot.materialmaker.mapper.TaskMapper;
import com.bot.materialmaker.model.Task;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动恢复：修复"应用在任务落库后、流水线执行前崩溃/重启"造成的遗留状态——
 * - PROCESSING：执行现场已丢失，无法续跑，标记为失败并给出原因；
 * - WAITING：已落库但从未执行，重新投递到流水线线程池。
 * 由此保证任何时刻提交成功的任务最终都会到达 SUCCESS/FAILED 终态。
 */
@Component
public class TaskRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskRecovery.class);

    private final TaskMapper taskMapper;
    private final TaskPipeline taskPipeline;
    private final Executor taskExecutor;

    public TaskRecovery(TaskMapper taskMapper, TaskPipeline taskPipeline, Executor taskExecutor) {
        this.taskMapper = taskMapper;
        this.taskPipeline = taskPipeline;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void run(ApplicationArguments args) {
        var interrupted = taskMapper.findByStatus("PROCESSING");
        for (Task task : interrupted) {
            log.warn("恢复：任务 {} 重启前处于 PROCESSING，标记为失败", task.getId());
            taskMapper.markFailed(task.getId(), task.getCurrentPhase(),
                    "服务重启导致任务中断，请重新提交", LocalDateTime.now());
        }

        var waiting = taskMapper.findByStatus("WAITING");
        for (Task task : waiting) {
            String taskId = task.getId();
            try {
                taskExecutor.execute(() -> taskPipeline.run(taskId));
                log.info("恢复：任务 {} 重新投递执行", taskId);
            } catch (RejectedExecutionException e) {
                // 队列满则保持 WAITING，下次重启或后续重试机制再处理
                log.warn("恢复：任务 {} 投递失败（队列满），保持 WAITING", taskId);
            }
        }
    }
}
