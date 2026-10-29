package com.bot.materialmaker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bot.materialmaker.common.TaskPhases;
import com.bot.materialmaker.common.TaskStatus;
import com.bot.materialmaker.config.SimulationProperties;
import com.bot.materialmaker.mapper.TaskMapper;
import com.bot.materialmaker.mapper.TaskStepMapper;
import com.bot.materialmaker.mapper.TemplateMapper;
import com.bot.materialmaker.model.Task;
import com.bot.materialmaker.model.Template;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 覆盖核心行为：三步执行顺序、失败中断、状态落库边界。 */
@ExtendWith(MockitoExtension.class)
class TaskPipelineTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskStepMapper taskStepMapper;
    @Mock
    private TemplateMapper templateMapper;
    @Mock
    private ResultStore resultStore;

    private SimulationProperties simulation;
    private TaskPipeline pipeline;

    @BeforeEach
    void setUp() {
        simulation = new SimulationProperties();
        simulation.setStepDelayMillis(Map.of(
                TaskPhases.MATERIAL_CHECK, 0L,
                TaskPhases.GENERATING, 0L,
                TaskPhases.ASSEMBLING, 0L));
        pipeline = new TaskPipeline(taskMapper, taskStepMapper, templateMapper, simulation, resultStore);
    }

    private Task waitingTask(String taskId) {
        Task task = new Task();
        task.setId(taskId);
        task.setUserId(1L);
        task.setTemplateId("tpl-1");
        task.setStatus(TaskStatus.WAITING);
        return task;
    }

    private Template template() {
        Template template = new Template();
        template.setId("tpl-1");
        template.setTitle("旅行拼图");
        template.setResultAsset("demo-collage.png");
        return template;
    }

    @Test
    @DisplayName("成功路径：按 素材检查→模拟生成→结果整理 顺序执行，最终任务 SUCCESS")
    void shouldRunAllStepsInOrderOnSuccess() throws IOException {
        Task task = waitingTask("t1");
        when(taskMapper.findById("t1")).thenReturn(task);
        when(templateMapper.findById("tpl-1")).thenReturn(template());
        when(resultStore.buildResult(eq("tpl-1"), anyString(), eq("t1")))
                .thenReturn("/files/results/t1.png");

        pipeline.run("t1");

        InOrder inOrder = inOrder(taskMapper, taskStepMapper);
        inOrder.verify(taskMapper).updateStatus("t1", TaskStatus.PROCESSING);
        inOrder.verify(taskMapper).updatePhase("t1", TaskPhases.MATERIAL_CHECK);
        inOrder.verify(taskStepMapper).markRunning(eq("t1"), eq(TaskPhases.MATERIAL_CHECK), any());
        inOrder.verify(taskStepMapper).markSuccess(eq("t1"), eq(TaskPhases.MATERIAL_CHECK), any());
        inOrder.verify(taskMapper).updatePhase("t1", TaskPhases.GENERATING);
        inOrder.verify(taskStepMapper).markRunning(eq("t1"), eq(TaskPhases.GENERATING), any());
        inOrder.verify(taskStepMapper).markSuccess(eq("t1"), eq(TaskPhases.GENERATING), any());
        inOrder.verify(taskMapper).updatePhase("t1", TaskPhases.ASSEMBLING);
        inOrder.verify(taskStepMapper).markRunning(eq("t1"), eq(TaskPhases.ASSEMBLING), any());
        inOrder.verify(taskStepMapper).markSuccess(eq("t1"), eq(TaskPhases.ASSEMBLING), any());
        inOrder.verify(taskMapper).markSuccess(eq("t1"), eq("/files/results/t1.png"), any());

        verify(taskMapper, never()).markFailed(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("失败注入：指定阶段失败后，后续步骤不再执行，任务标记 FAILED 并记录原因")
    void shouldStopFollowingStepsWhenInjectedFailureHits() throws IOException {
        simulation.setFailPhase(TaskPhases.GENERATING);
        when(taskMapper.findById("t1")).thenReturn(waitingTask("t1"));
        when(templateMapper.findById("tpl-1")).thenReturn(template());

        pipeline.run("t1");

        // 前一步正常完成
        verify(taskStepMapper).markSuccess(eq("t1"), eq(TaskPhases.MATERIAL_CHECK), any());
        // 失败阶段被标记 FAILED
        verify(taskStepMapper).markFailed(eq("t1"), eq(TaskPhases.GENERATING), any(), anyString());
        verify(taskMapper).markFailed(eq("t1"), eq(TaskPhases.GENERATING), anyString(), any());
        // 后续阶段完全不执行
        verify(taskStepMapper, never()).markRunning(eq("t1"), eq(TaskPhases.ASSEMBLING), any());
        verify(taskStepMapper, never()).markSuccess(eq("t1"), eq(TaskPhases.ASSEMBLING), any());
        verify(taskMapper, never()).markSuccess(anyString(), anyString(), any());
        verify(resultStore, never()).buildResult(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("已完成任务不因重复投递再次执行")
    void shouldSkipTaskThatIsNotWaiting() {
        Task task = waitingTask("t1");
        task.setStatus(TaskStatus.SUCCESS);
        when(taskMapper.findById("t1")).thenReturn(task);

        pipeline.run("t1");

        verify(taskMapper, never()).updateStatus(anyString(), anyString());
        verify(taskStepMapper, never()).markRunning(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("结果整理异常会将任务标记为该阶段失败")
    void shouldFailTaskWhenResultAssemblyThrows() throws IOException {
        when(taskMapper.findById("t1")).thenReturn(waitingTask("t1"));
        when(templateMapper.findById("tpl-1")).thenReturn(template());
        when(resultStore.buildResult(anyString(), anyString(), anyString()))
                .thenThrow(new IOException("磁盘写入失败"));

        pipeline.run("t1");

        verify(taskMapper).markFailed(eq("t1"), eq(TaskPhases.ASSEMBLING), anyString(), any());
        verify(taskMapper, never()).markSuccess(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("delayFor 读取每阶段配置，缺省 1500ms")
    void shouldReadStepDelayConfig() {
        assertEquals(0L, simulation.delayFor(TaskPhases.GENERATING));
        SimulationProperties defaults = new SimulationProperties();
        assertEquals(1500L, defaults.delayFor("UNKNOWN_PHASE"));
    }
}
