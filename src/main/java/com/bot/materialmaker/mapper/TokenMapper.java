package com.bot.materialmaker.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TokenMapper {

    @Insert("INSERT INTO auth_tokens(token, user_id) VALUES(#{token}, #{userId})")
    int insert(@Param("token") String token, @Param("userId") Long userId);

    /** 仅接受未过期（7 天内签发）的 token */
    @Select("SELECT user_id FROM auth_tokens "
            + "WHERE token = #{token} AND created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)")
    Long findUserIdByToken(String token);

    /** 登出：删除指定 token */
    @Delete("DELETE FROM auth_tokens WHERE token = #{token}")
    int deleteByToken(String token);

    /** 清理已过期 token（登录时顺带执行，控制表体积） */
    @Delete("DELETE FROM auth_tokens WHERE created_at <= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    int deleteExpired();
}
