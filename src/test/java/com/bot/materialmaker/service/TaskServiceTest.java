package com.bot.materialmaker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.common.TaskPhases;
import com.bot.materialmaker.common.TaskStatus;
import com.bot.materialmaker.dto.SubmitTaskRequest;
import com.bot.materialmaker.dto.SubmitTaskResponse;
import com.bot.materialmaker.mapper.MaterialMapper;
import com.bot.materialmaker.mapper.TaskMapper;
import com.bot.materialmaker.mapper.TaskStepMapper;
import com.bot.materialmaker.mapper.TemplateMapper;
import com.bot.materialmaker.model.Material;
import com.bot.materialmaker.model.Template;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 任务受理的前置校验：模板存在性、素材数量/归属/类型/大小。 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskStepMapper taskStepMapper;
    @Mock
    private TemplateMapper templateMapper;
    @Mock
    private MaterialMapper materialMapper;

    @InjectMocks
    private TaskService taskService;

    private Template template(String id, int requiredCount) {
        Template t = new Template();
        t.setId(id);
        t.setTitle("旅行拼图");
        t.setRequiredImageCount(requiredCount);
        t.setAcceptedMimeTypes("image/jpeg,image/png");
        t.setMaxFileSizeBytes(5242880L);
        return t;
    }

    private Material material(long id, Long userId, String mime, long size) {
        Material m = new Material();
        m.setId(id);
        m.setUserId(userId);
        m.setMimeType(mime);
        m.setSizeBytes(size);
        m.setOriginalName("img-" + id + ".png");
        return m;
    }

    private SubmitTaskRequest request(String templateId, List<Long> ids) {
        SubmitTaskRequest r = new SubmitTaskRequest();
        r.setTemplateId(templateId);
        r.setMaterialIds(ids);
        return r;
    }

    @Test
    @DisplayName("模板不存在时返回 404")
    void shouldRejectWhenTemplateMissing() {
        when(templateMapper.findById("nope")).thenReturn(null);
        ApiException e = assertThrows(ApiException.class,
                () -> taskService.submit(1L, request("nope", List.of(1L))));
        assertEquals(404, e.getStatus().value());
    }

    @Test
    @DisplayName("素材数量与模板要求不符时拒绝")
    void shouldRejectWrongMaterialCount() {
        when(templateMapper.findById("tpl-1")).thenReturn(template("tpl-1", 2));
        ApiException e = assertThrows(ApiException.class,
                () -> taskService.submit(1L, request("tpl-1", List.of(1L))));
        assertEquals(400, e.getStatus().value());
        verify(taskMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("素材不属于当前用户时拒绝")
    void shouldRejectForeignMaterial() {
        when(templateMapper.findById("tpl-1")).thenReturn(template("tpl-1", 1));
        when(materialMapper.findByIds(List.of(9L)))
                .thenReturn(List.of(material(9L, 2L, "image/png", 1000)));
        ApiException e = assertThrows(ApiException.class,
                () -> taskService.submit(1L, request("tpl-1", List.of(9L))));
        assertEquals(403, e.getStatus().value());
    }

    @Test
    @DisplayName("素材类型不符合模板要求时拒绝")
    void shouldRejectWrongMimeType() {
        when(templateMapper.findById("tpl-1")).thenReturn(template("tpl-1", 1));
        when(materialMapper.findByIds(List.of(9L)))
                .thenReturn(List.of(material(9L, 1L, "image/gif", 1000)));
        ApiException e = assertThrows(ApiException.class,
                () -> taskService.submit(1L, request("tpl-1", List.of(9L))));
        assertEquals(400, e.getStatus().value());
    }

    @Test
    @DisplayName("校验通过：创建 WAITING 任务并登记三个 PENDING 步骤")
    void shouldCreateWaitingTaskWithPendingSteps() {
        when(templateMapper.findById("tpl-1")).thenReturn(template("tpl-1", 1));
        when(materialMapper.findByIds(List.of(9L)))
                .thenReturn(List.of(material(9L, 1L, "image/png", 1000)));

        SubmitTaskResponse response = taskService.submit(1L, request("tpl-1", List.of(9L)));

        assertEquals(TaskStatus.WAITING, response.getStatus());
        verify(taskMapper).insert(org.mockito.ArgumentMatchers.any());
        verify(taskStepMapper).insert(argThat(s -> TaskPhases.MATERIAL_CHECK.equals(s.getStep())));
        verify(taskStepMapper).insert(argThat(s -> TaskPhases.GENERATING.equals(s.getStep())));
        verify(taskStepMapper).insert(argThat(s -> TaskPhases.ASSEMBLING.equals(s.getStep())));
    }
}
