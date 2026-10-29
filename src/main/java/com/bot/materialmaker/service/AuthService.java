package com.bot.materialmaker.service;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.dto.LoginResponse;
import com.bot.materialmaker.mapper.TokenMapper;
import com.bot.materialmaker.mapper.UserMapper;
import com.bot.materialmaker.model.User;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示用的轻量身份方案：用户输入用户名即完成登录（无密码），
 * 服务端生成随机令牌返回，前端保存并在后续请求中通过 X-Auth-Token 携带。
 *
 * 安全要点：
 * - token 有效期 7 天（由 TokenMapper 查询条件保证），登录时顺带清理过期 token；
 * - 对同一用户名的登录做内存级限流，缓解刷用户/刷 token 的滥用；
 * - 并发首次登录同一用户名时捕获唯一键冲突并复用已有用户，而不是报 500。
 */
@Service
public class AuthService {

    /** 同一用户名登录限流窗口 */
    private static final long WINDOW_MILLIS = 60_000L;
    /** 窗口内同一用户名最大登录次数 */
    private static final int MAX_LOGINS_PER_WINDOW = 10;

    private final UserMapper userMapper;
    private final TokenMapper tokenMapper;
    /** username -> 窗口起始时间戳，仅存内存（重启即清零，够演示防滥用用） */
    private final ConcurrentHashMap<String, WindowCounter> loginCounters = new ConcurrentHashMap<>();

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
        checkRateLimit(name);

        User user = userMapper.findByUsername(name);
        if (user == null) {
            user = new User();
            user.setUsername(name);
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                // 并发首次登录同一用户名：另一请求已建号，直接复用
                user = userMapper.findByUsername(name);
                if (user == null) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "登录失败，请重试");
                }
            }
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenMapper.insert(token, user.getId());
        // 顺带清理过期 token，避免 auth_tokens 无限增长
        tokenMapper.deleteExpired();
        return new LoginResponse(token, user.getUsername());
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenMapper.deleteByToken(token.trim());
        }
    }

    private void checkRateLimit(String username) {
        long now = System.currentTimeMillis();
        WindowCounter counter = loginCounters.compute(username, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MILLIS) {
                return new WindowCounter(now, 1);
            }
            existing.count++;
            return existing;
        });
        if (counter.count > MAX_LOGINS_PER_WINDOW) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "登录过于频繁，请稍后再试");
        }
    }

    private static final class WindowCounter {
        final long windowStart;
        int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
