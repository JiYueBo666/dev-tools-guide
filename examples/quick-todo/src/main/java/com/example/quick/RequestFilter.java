package com.example.quick;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class RequestFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);
    private final byte[] apiKey;
    private final int perSecond;
    private long window = System.currentTimeMillis() / 1000;
    private int count;
    public RequestFilter(@Value("${app.api-key}") String key, @Value("${app.requests-per-second:60}") int limit) {
        if (key.isBlank() || limit < 1) throw new IllegalArgumentException("配置不能为空或小于1");
        this.apiKey = key.getBytes(StandardCharsets.UTF_8);
        this.perSecond = limit;
    }
    // 单进程全局固定窗口；多人/多实例项目需要换成合适的限流组件。
    private synchronized boolean allow() {
        long now = System.currentTimeMillis() / 1000;
        if (now != window) { window = now; count = 0; }
        return ++count <= perSecond;
    }
    private void fail(HttpServletResponse r, int status, String code, String message, String id) throws IOException {
        r.setStatus(status);
        r.setContentType("application/json");
        r.setCharacterEncoding("UTF-8");
        // 参数只取下方固定字符串与服务端生成的 UUID。
        r.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"requestId\":\"" + id + "\"}");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        request.setAttribute("requestId", id);
        response.setHeader("X-Request-ID", id);
        MDC.put("requestId", id);
        long start = System.nanoTime();
        try {
            if (request.getMethod().equals("OPTIONS")) { chain.doFilter(request, response); return; }
            String supplied = request.getHeader("X-API-Key");
            if (supplied == null || !MessageDigest.isEqual(apiKey, supplied.getBytes(StandardCharsets.UTF_8))) {
                fail(response, 401, "UNAUTHORIZED", "缺少有效凭证", id); return;
            }
            if (!allow()) {
                response.setHeader("Retry-After", "1");
                fail(response, 429, "RATE_LIMITED", "请求太快，请稍后再试", id); return;
            }
            chain.doFilter(request, response);
        } finally {
            log.info("method={} path={} status={} durationMs={}", request.getMethod(), request.getRequestURI(),
                response.getStatus(), (System.nanoTime() - start)/1_000_000);
            MDC.remove("requestId");
        }
    }
}
