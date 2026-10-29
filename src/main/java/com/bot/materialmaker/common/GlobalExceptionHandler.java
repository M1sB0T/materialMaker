package com.bot.materialmaker.common;

import java.time.Instant;
import java.util.Map;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e) {
        return body(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不合法"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "上传文件过大");
    }

    /** 请求的静态资源（封面图 / 结果图）不存在时返回 404，而非落入兜底 500 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "资源不存在");
    }

    /** 客户端中断下载（浏览器关闭预览等）属正常现象，静默处理，不写响应体 */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort() {
        // 无需任何响应，直接结束
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误：" + e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "message", message));
    }
}
