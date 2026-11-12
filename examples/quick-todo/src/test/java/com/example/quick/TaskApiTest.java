package com.example.quick;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class TaskApiTest {
    @Autowired MockMvc mvc;
    @Autowired TaskRepository repository;
    @Autowired TaskService service;
    @BeforeEach void clear() { repository.deleteAll(); }
    @Test void crudAndFalseAndSoftDelete() throws Exception {
        String location = mvc.perform(post("/api/tasks").header("X-API-Key","test-key")
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"练习\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(patch(location).header("X-API-Key","test-key").contentType(MediaType.APPLICATION_JSON)
            .content("{\"done\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.done").value(true));
        mvc.perform(patch(location).header("X-API-Key","test-key").contentType(MediaType.APPLICATION_JSON)
            .content("{\"done\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.done").value(false));
        mvc.perform(get("/api/tasks?done=false&page=1&size=1").header("X-API-Key","test-key"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(delete(location).header("X-API-Key","test-key")).andExpect(status().isNoContent());
        mvc.perform(get(location).header("X-API-Key","test-key")).andExpect(status().isNotFound());
        Assertions.assertEquals(1,repository.count());
        Assertions.assertTrue(repository.findAll().getFirst().isDeleted());
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"content\":\" \"}","bad-json"})
    void validatesInput(String input) throws Exception {
        mvc.perform(post("/api/tasks").header("X-API-Key","test-key").contentType(MediaType.APPLICATION_JSON)
            .content(input)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.requestId").isNotEmpty());
    }
    @Test void auth() throws Exception { mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized()); }
    @Test void badPage() throws Exception {
        mvc.perform(get("/api/tasks?page=0").header("X-API-Key","test-key")).andExpect(status().isBadRequest());
    }
    @Test void corsPreflight() throws Exception {
        mvc.perform(options("/api/tasks").header("Origin","http://127.0.0.1:8080")
            .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","X-API-Key,Content-Type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:8080"));
    }
    @Test void imports() throws Exception {
        mvc.perform(post("/api/tasks/imports").header("X-API-Key","test-key").contentType(MediaType.APPLICATION_JSON)
            .content("{\"contents\":[\"任务甲\",\"任务乙\"]}")).andExpect(status().isOk());
        Assertions.assertEquals(2,repository.count());
    }
    @Test void rollsBackWholeImport() {
        Assertions.assertThrows(RuntimeException.class,
            () -> service.importTasks(java.util.List.of("先写入", "x".repeat(201))));
        Assertions.assertEquals(0, repository.count());
    }
    @Test void uploadsAndRejectsEmptyFile() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        var result = mvc.perform(multipart("/api/files").file(file).header("X-API-Key","test-key"))
            .andExpect(status().isCreated()).andReturn();
        String json = result.getResponse().getContentAsString();
        // 固定响应只有 fileId，可直接提取 UUID；不依赖字段顺序解析其他数据。
        String id = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f-]{27,}").matcher(json).results().findFirst().orElseThrow().group();
        var path = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "quick-todo-test-files", id);
        try { Assertions.assertEquals("hello", java.nio.file.Files.readString(path)); }
        finally { java.nio.file.Files.deleteIfExists(path); }
        mvc.perform(multipart("/api/files").file(new org.springframework.mock.web.MockMultipartFile("file", new byte[0]))
            .header("X-API-Key","test-key")).andExpect(status().isBadRequest());
    }
    @Test void fixedWindowLimiterRejectsSecondRequest() throws Exception {
        var filter = new RequestFilter("test-key", 1);
        java.util.function.Supplier<org.springframework.mock.web.MockHttpServletRequest> request = () -> {
            var r = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/tasks");
            r.addHeader("X-API-Key", "test-key"); return r;
        };
        // 同一窗口内的两次请求；若正好跨秒则重新执行一组。
        boolean rejected = false;
        for (int i = 0; i < 3 && !rejected; i++) {
            filter.doFilter(request.get(), new org.springframework.mock.web.MockHttpServletResponse(), (a,b) -> {});
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(request.get(), response, (a,b) -> {});
            rejected = response.getStatus() == 429;
        }
        Assertions.assertTrue(rejected);
    }
}
