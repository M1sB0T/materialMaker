package com.bot.materialmaker.mapper;

import com.bot.materialmaker.model.TaskStep;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TaskStepMapper {

    @Insert("INSERT INTO task_steps(task_id, step, status) VALUES(#{taskId}, #{step}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskStep step);

    @Select("SELECT * FROM task_steps WHERE task_id = #{taskId} ORDER BY id")
    List<TaskStep> findByTask(String taskId);

    /** 批量查询多个任务的步骤（避免任务列表页 N+1 查询） */
    @Select("<script>SELECT * FROM task_steps WHERE task_id IN "
            + "<foreach item='item' collection='taskIds' open='(' separator=',' close=')'>#{item}</foreach>"
            + " ORDER BY id</script>")
    List<TaskStep> findByTaskIds(@Param("taskIds") List<String> taskIds);

    @Update("UPDATE task_steps SET status = 'RUNNING', started_at = #{startedAt} "
            + "WHERE task_id = #{taskId} AND step = #{step}")
    int markRunning(@Param("taskId") String taskId, @Param("step") String step,
                    @Param("startedAt") LocalDateTime startedAt);

    @Update("UPDATE task_steps SET status = 'SUCCESS', finished_at = #{finishedAt} "
            + "WHERE task_id = #{taskId} AND step = #{step}")
    int markSuccess(@Param("taskId") String taskId, @Param("step") String step,
                    @Param("finishedAt") LocalDateTime finishedAt);

    @Update("UPDATE task_steps SET status = 'FAILED', finished_at = #{finishedAt}, message = #{message} "
            + "WHERE task_id = #{taskId} AND step = #{step}")
    int markFailed(@Param("taskId") String taskId, @Param("step") String step,
                   @Param("finishedAt") LocalDateTime finishedAt, @Param("message") String message);
}
