package com.bot.materialmaker.mapper;

import com.bot.materialmaker.model.Material;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MaterialMapper {

    @Insert("INSERT INTO materials(user_id, original_name, stored_path, mime_type, size_bytes) "
            + "VALUES(#{userId}, #{originalName}, #{storedPath}, #{mimeType}, #{sizeBytes})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Material material);

    @Select("SELECT * FROM materials WHERE id = #{id}")
    Material findById(Long id);

    @Select("<script>SELECT * FROM materials WHERE id IN "
            + "<foreach item='item' collection='ids' open='(' separator=',' close=')'>#{item}</foreach>"
            + "</script>")
    List<Material> findByIds(@Param("ids") List<Long> ids);
}
