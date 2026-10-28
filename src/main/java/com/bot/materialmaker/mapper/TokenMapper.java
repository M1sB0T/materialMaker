package com.bot.materialmaker.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TokenMapper {

    @Insert("INSERT INTO auth_tokens(token, user_id) VALUES(#{token}, #{userId})")
    int insert(@Param("token") String token, @Param("userId") Long userId);

    @Select("SELECT user_id FROM auth_tokens WHERE token = #{token}")
    Long findUserIdByToken(String token);
}
