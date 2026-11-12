import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HttpPractice {
    private static final String ORIGIN = "http://127.0.0.1:8080";
    private static final boolean CORS = "1".equals(System.getenv("ALLOW_CORS"));
    private static final String TAG = "\"tips-v1\"";

    public static void main(String[] args) throws IOException {
        HttpServer web = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        HttpServer api = HttpServer.create(new InetSocketAddress("127.0.0.1", 8081), 0);
        web.createContext("/", HttpPractice::page);
        api.createContext("/", HttpPractice::handle);
        api.start();
        web.start();
        System.out.println("网页：http://127.0.0.1:8080");
        System.out.println("接口：http://127.0.0.1:8081/api/tasks；CORS：" + CORS);
        System.out.println("按 Control + C 停止。此实验不连接数据库。");
    }

    private static void page(HttpExchange x) throws IOException {
        String body = """
            <!doctype html><html lang="zh-CN"><meta charset="utf-8">
            <title>HTTP 观察实验</title>
            <h1>请求从哪里来，又到哪里去？</h1>
            <p>先打开 Network 面板，再点击按钮。</p>
            <button onclick="run(false)">读取任务：GET</button>
            <button onclick="run(true)">发送 JSON：POST</button>
            <pre id="result">等待操作</pre>
            <script>
            async function run(post) {
              const result = document.getElementById('result');
              try {
                const url = 'http://127.0.0.1:8081' + (post ? '/api/echo' : '/api/tasks');
                const options = post ? {
                  method: 'POST', headers: {'Content-Type': 'application/json'},
                  body: JSON.stringify({content: '学习 HTTP'})
                } : {};
                const response = await fetch(url, options);
                result.textContent = 'HTTP ' + response.status + '\\n' + await response.text();
              } catch (error) {
                result.textContent = '网页无法读取响应，请查看 Console 和 Network。\\n' + error;
              }
            }
            </script></html>
            """;
        send(x, 200, "text/html; charset=utf-8", body);
    }

    private static void handle(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        System.out.println(x.getRequestMethod() + " " + path
                + " Origin=" + x.getRequestHeaders().getFirst("Origin"));
        x.getResponseHeaders().set("Cache-Control", "no-store");
        if (CORS && ORIGIN.equals(x.getRequestHeaders().getFirst("Origin"))) {
            x.getResponseHeaders().set("Access-Control-Allow-Origin", ORIGIN);
            x.getResponseHeaders().set("Vary", "Origin");
            x.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            x.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        }
        if (x.getRequestMethod().equals("OPTIONS")) {
            x.sendResponseHeaders(204, -1);
            x.close();
            return;
        }
        if (path.equals("/tips") && x.getRequestMethod().equals("GET")) {
            x.getResponseHeaders().set("Cache-Control", "public, max-age=30");
            x.getResponseHeaders().set("ETag", TAG);
            if (TAG.equals(x.getRequestHeaders().getFirst("If-None-Match"))) {
                x.sendResponseHeaders(304, -1);
                x.close();
            } else {
                send(x, 200, "text/plain; charset=utf-8", "先理解一次请求，再背协议名称。\n");
            }
        } else if (path.equals("/api/tasks") && x.getRequestMethod().equals("GET")) {
            send(x, 200, "application/json", "[{\"id\":1,\"content\":\"学习 HTTP\",\"done\":false}]");
        } else if (path.equals("/api/echo") && x.getRequestMethod().equals("POST")) {
            // 将收到的正文作为文字返回，方便观察中文和请求体；不解析 JSON。
            String body = new String(x.getRequestBody().readNBytes(8192), StandardCharsets.UTF_8);
            send(x, 200, "text/plain; charset=utf-8", body);
        } else {
            send(x, 404, "application/json", "{\"code\":\"NOT_FOUND\",\"message\":\"没有这个实验地址\"}");
        }
    }

    private static void send(HttpExchange x, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(status, bytes.length);
        try (var output = x.getResponseBody()) {
            output.write(bytes);
        }
        x.close();
    }
}
