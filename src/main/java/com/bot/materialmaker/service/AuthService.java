package com.bot.materialmaker.service;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.dto.LoginResponse;
import com.bot.materialmaker.mapper.TokenMapper;
import com.bot.materialmaker.mapper.UserMapper;
import com.bot.materialmaker.model.User;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示用的轻量身份方案：用户输入用户名即完成登录（无密码），
 * 服务端生成随机令牌返回，前端保存并在后续请求中通过 X-Auth-Token 携带。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final TokenMapper tokenMapper;

    public AuthService(UserMapper userMapper, TokenMapper tokenMapper) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
    }

    @Transactional
    public LoginResponse login(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        User user = userMapper.findByUsername(name);
        if (user == null) {
            user = new User();
            user.setUsername(name);
            userMapper.insert(user);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenMapper.insert(token, user.getId());
        return new LoginResponse(token, user.getUsername());
    }
}
