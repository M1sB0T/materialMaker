package com.bot.materialmaker.mapper;

import com.bot.materialmaker.model.Template;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface TemplateMapper {

    @Select("SELECT * FROM templates ORDER BY created_at")
    List<Template> findAll();

    @Select("SELECT * FROM templates WHERE id = #{id}")
    Template findById(String id);
}
