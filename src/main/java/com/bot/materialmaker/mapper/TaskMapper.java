package com.bot.materialmaker.mapper;

import com.bot.materialmaker.model.Task;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TaskMapper {

    @Insert("INSERT INTO tasks(id, user_id, template_id, status) "
            + "VALUES(#{id}, #{userId}, #{templateId}, #{status})")
    int insert(Task task);

    @Select("SELECT * FROM tasks WHERE id = #{id}")
    Task findById(String id);

    @Select("SELECT * FROM tasks WHERE user_id = #{userId} ORDER BY created_at DESC, id DESC")
    List<Task> findByUser(Long userId);

    @Update("UPDATE tasks SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    @Update("UPDATE tasks SET current_phase = #{phase} WHERE id = #{id}")
    int updatePhase(@Param("id") String id, @Param("phase") String phase);

    @Update("UPDATE tasks SET status = 'FAILED', current_phase = #{failPhase}, fail_phase = #{failPhase}, "
            + "fail_reason = #{failReason}, finished_at = #{finishedAt} WHERE id = #{id}")
    int markFailed(@Param("id") String id, @Param("failPhase") String failPhase,
                   @Param("failReason") String failReason, @Param("finishedAt") LocalDateTime finishedAt);

    @Update("UPDATE tasks SET status = 'SUCCESS', current_phase = 'ASSEMBLING', result_url = #{resultUrl}, "
            + "fail_reason = NULL, finished_at = #{finishedAt} WHERE id = #{id}")
    int markSuccess(@Param("id") String id, @Param("resultUrl") String resultUrl,
                    @Param("finishedAt") LocalDateTime finishedAt);
}
