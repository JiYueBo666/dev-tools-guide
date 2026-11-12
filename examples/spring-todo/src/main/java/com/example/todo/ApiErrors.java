package com.example.todo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);
    public record ErrorBody(String code, String message, Object requestId) {}
    private ResponseEntity<ErrorBody> error(int status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message, request.getAttribute("requestId")));
    }
    @ExceptionHandler(TaskService.TaskMissing.class)
    public ResponseEntity<ErrorBody> missing(HttpServletRequest request) {
        return error(404, "TASK_NOT_FOUND", "没有找到这条任务", request);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var field = ex.getBindingResult().getFieldError();
        return error(400, "INVALID_INPUT", field == null ? "请检查输入" : field.getDefaultMessage(), request);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorBody> malformed(HttpServletRequest request) {
        return error(400, "INVALID_REQUEST", "请求格式不正确", request);
    }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorBody> method(org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(405).allow(ex.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new))
                .body(new ErrorBody("METHOD_NOT_ALLOWED", "这个地址不接受此请求方法", request.getAttribute("requestId")));
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorBody> media(HttpServletRequest request) {
        return error(415, "UNSUPPORTED_MEDIA_TYPE", "请检查请求内容类型", request);
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorBody> route(HttpServletRequest request) {
        return error(404, "ROUTE_NOT_FOUND", "没有找到这个地址", request);
    }
    // 最后的兜底：记录意外异常，不把内部堆栈发给用户。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> unexpected(Exception ex, HttpServletRequest request) {
        log.error("request failed", ex);
        return error(500, "INTERNAL_ERROR", "操作暂时失败，请稍后再试", request);
    }
}
