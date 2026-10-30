package com.bot.materialmaker.common;

import org.springframework.http.HttpStatus;

/** 业务异常，由全局异常处理器转换为统一 JSON 响应。 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
