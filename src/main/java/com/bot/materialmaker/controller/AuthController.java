package com.bot.materialmaker.controller;

import com.bot.materialmaker.dto.LoginRequest;
import com.bot.materialmaker.dto.LoginResponse;
import com.bot.materialmaker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getUsername());
    }

    /** 登出：吊销当前请求携带的 token（幂等，未登录也返回成功） */
    @DeleteMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("X-Auth-Token"));
        return Map.of("status", HttpStatus.OK.value(), "message", "已退出登录");
    }
}
