package com.bot.materialmaker.controller;

import com.bot.materialmaker.auth.AuthInterceptor;
import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.dto.SubmitTaskRequest;
import com.bot.materialmaker.dto.SubmitTaskResponse;
import com.bot.materialmaker.dto.TaskView;
import com.bot.materialmaker.service.TaskPipeline;
import com.bot.materialmaker.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务接口：提交接口完成校验与落库后立即返回任务 ID，
 * 后台流水线由独立线程池继续执行（受理与执行分离）。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskPipeline taskPipeline;
    private final Executor taskExecutor;

    public TaskController(TaskService taskService, TaskPipeline taskPipeline, Executor taskExecutor) {
        this.taskService = taskService;
        this.taskPipeline = taskPipeline;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping
    public SubmitTaskResponse submit(@Valid @RequestBody SubmitTaskRequest request,
                                     HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTR);
        // 事务方法返回时任务与步骤记录已提交，之后才投递后台执行
        SubmitTaskResponse response = taskService.submit(userId, request);
        try {
            taskExecutor.execute(() -> taskPipeline.run(response.getTaskId()));
        } catch (RejectedExecutionException e) {
            // 投递失败必须把任务落到终态，否则会永远停留在 WAITING
            taskService.failTask(response.getTaskId(), "系统繁忙，任务未能开始执行，请重新提交");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "系统繁忙，请稍后重新提交");
        }
        return response;
    }

    @GetMapping
    public List<TaskView> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        return taskService.listByUser(userId);
    }

    @GetMapping("/{taskId}")
    public TaskView detail(@PathVariable String taskId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        return taskService.get(userId, taskId);
    }
}
