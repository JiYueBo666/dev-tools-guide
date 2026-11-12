package com.example.quick;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);
    private ResponseEntity<ApiError> reply(int status, String code, String message, HttpServletRequest r) {
        return ResponseEntity.status(status).body(new ApiError(code, message, r.getAttribute("requestId")));
    }
    @ExceptionHandler(TaskService.Missing.class)
    public ResponseEntity<ApiError> missing(HttpServletRequest r) {
        return reply(404, "TASK_NOT_FOUND", "没有找到任务", r);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> invalid(HttpServletRequest r) {
        return reply(400, "INVALID_INPUT", "请检查字段、格式和取值范围", r);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> large(HttpServletRequest r) { return reply(413, "FILE_TOO_LARGE", "文件过大", r); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> media(HttpServletRequest r) { return reply(415, "WRONG_CONTENT_TYPE", "请求类型不支持", r); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
        return ResponseEntity.status(405).allow(e.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new))
            .body(new ApiError("METHOD_NOT_ALLOWED", "请求方法不支持", r.getAttribute("requestId")));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> route(HttpServletRequest r) { return reply(404, "ROUTE_NOT_FOUND", "地址不存在", r); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest r) {
        log.error("request failed", e);
        return reply(500, "INTERNAL_ERROR", "操作失败，请稍后重试", r);
    }
}
