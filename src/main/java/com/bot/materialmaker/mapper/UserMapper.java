package com.bot.materialmaker.mapper;

import com.bot.materialmaker.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO users(username) VALUES(#{username})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
