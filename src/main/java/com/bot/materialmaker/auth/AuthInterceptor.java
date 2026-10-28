package com.bot.materialmaker.auth;

import com.bot.materialmaker.mapper.TokenMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 简单令牌认证：从 X-Auth-Token 头解析用户，注入 request attribute。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTR = "userId";

    private final TokenMapper tokenMapper;

    public AuthInterceptor(TokenMapper tokenMapper) {
        this.tokenMapper = tokenMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            return reject(response, "未登录，请先登录");
        }
        Long userId = tokenMapper.findUserIdByToken(token.trim());
        if (userId == null) {
            return reject(response, "登录凭证无效，请重新登录");
        }
        request.setAttribute(USER_ID_ATTR, userId);
        return true;
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
