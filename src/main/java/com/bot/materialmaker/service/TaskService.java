package com.bot.materialmaker.service;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.common.TaskPhases;
import com.bot.materialmaker.common.TaskStatus;
import com.bot.materialmaker.dto.TaskStepView;
import com.bot.materialmaker.dto.TaskView;
import com.bot.materialmaker.dto.SubmitTaskRequest;
import com.bot.materialmaker.dto.SubmitTaskResponse;
import com.bot.materialmaker.mapper.MaterialMapper;
import com.bot.materialmaker.mapper.TaskMapper;
import com.bot.materialmaker.mapper.TaskStepMapper;
import com.bot.materialmaker.mapper.TemplateMapper;
import com.bot.materialmaker.model.Material;
import com.bot.materialmaker.model.Task;
import com.bot.materialmaker.model.TaskStep;
import com.bot.materialmaker.model.Template;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务受理与查询。受理接口只做参数校验、创建任务与步骤记录（WAITING），
 * 实际流水线由 TaskPipeline 在后台线程执行。
 */
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskStepMapper taskStepMapper;
    private final TemplateMapper templateMapper;
    private final MaterialMapper materialMapper;

    public TaskService(TaskMapper taskMapper, TaskStepMapper taskStepMapper,
                       TemplateMapper templateMapper, MaterialMapper materialMapper) {
        this.taskMapper = taskMapper;
        this.taskStepMapper = taskStepMapper;
        this.templateMapper = templateMapper;
        this.materialMapper = materialMapper;
    }

    @Transactional
    public SubmitTaskResponse submit(Long userId, SubmitTaskRequest request) {
        if (request == null || request.getTemplateId() == null || request.getTemplateId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "缺少模板 ID");
        }
        Template template = templateMapper.findById(request.getTemplateId());
        if (template == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "模板不存在");
        }
        List<Long> materialIds = request.getMaterialIds();
        if (materialIds == null || materialIds.size() != template.getRequiredImageCount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "素材数量不符合模板要求：需要 " + template.getRequiredImageCount() + " 张");
        }
        if (new HashSet<>(materialIds).size() != materialIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "素材存在重复引用");
        }
        List<Material> materials = materialMapper.findByIds(materialIds);
        if (materials.size() != materialIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "存在无效或已不存在的素材");
        }
        for (Material material : materials) {
            if (!userId.equals(material.getUserId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "素材不属于当前用户");
            }
            if (!template.acceptedMimeList().contains(material.getMimeType())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "素材类型不符合模板要求：" + material.getOriginalName());
            }
            if (material.getSizeBytes() != null
                    && material.getSizeBytes() > template.getMaxFileSizeBytes()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "素材超过大小限制：" + material.getOriginalName());
            }
        }

        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        task.setUserId(userId);
        task.setTemplateId(template.getId());
        task.setStatus(TaskStatus.WAITING);
        taskMapper.insert(task);

        for (String phase : TaskPhases.ALL) {
            TaskStep step = new TaskStep();
            step.setTaskId(task.getId());
            step.setStep(phase);
            step.setStatus("PENDING");
            taskStepMapper.insert(step);
        }
        return new SubmitTaskResponse(task.getId(), task.getStatus());
    }

    public List<TaskView> listByUser(Long userId) {
        return taskMapper.findByUser(userId).stream().map(this::toView).toList();
    }

    public TaskView get(Long userId, String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "任务不存在");
        }
        return toView(task);
    }

    public TaskView toView(Task task) {
        TaskView view = new TaskView();
        view.setId(task.getId());
        view.setTemplateId(task.getTemplateId());
        Template template = templateMapper.findById(task.getTemplateId());
        view.setTemplateTitle(template == null ? task.getTemplateId() : template.getTitle());
        view.setStatus(task.getStatus());
        view.setCurrentPhase(task.getCurrentPhase());
        view.setCurrentPhaseDisplay(TaskPhases.display(task.getCurrentPhase()));
        view.setFailPhase(task.getFailPhase());
        view.setFailReason(task.getFailReason());
        view.setResultUrl(task.getResultUrl());
        view.setCreatedAt(task.getCreatedAt() == null ? null : task.getCreatedAt().toString());
        view.setUpdatedAt(task.getUpdatedAt() == null ? null : task.getUpdatedAt().toString());
        view.setFinishedAt(task.getFinishedAt() == null ? null : task.getFinishedAt().toString());
        view.setSteps(taskStepMapper.findByTask(task.getId()).stream().map(this::toStepView).toList());
        return view;
    }

    private TaskStepView toStepView(TaskStep step) {
        TaskStepView v = new TaskStepView();
        v.setStep(step.getStep());
        v.setStepDisplay(TaskPhases.display(step.getStep()));
        v.setStatus(step.getStatus());
        v.setMessage(step.getMessage());
        v.setStartedAt(step.getStartedAt() == null ? null : step.getStartedAt().toString());
        v.setFinishedAt(step.getFinishedAt() == null ? null : step.getFinishedAt().toString());
        return v;
    }
}
