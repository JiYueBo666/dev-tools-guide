package com.example.todo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
class TaskHttpTest {
    @Autowired MockMvc mvc;
    @Test void createsAndReadsTask() throws Exception {
        String location = mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"学习框架\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.content").value("学习框架"))
                .andExpect(header().exists("X-Request-ID")).andReturn().getResponse().getHeader("Location");
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.done").value(false));
        mvc.perform(patch(location + "/done")).andExpect(status().isOk()).andExpect(jsonPath("$.done").value(true));
        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }
    @ParameterizedTest @ValueSource(strings={"{\"content\":\"\"}","{\"content\":\"   \"}","{}","{broken"})
    void rejectsBadInput(String body) throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.requestId").isNotEmpty());
    }
    @Test void preservesHttpErrors() throws Exception {
        mvc.perform(put("/api/tasks")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/missing")).andExpect(status().isNotFound());
        mvc.perform(post("/api/tasks").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }
    @Test void rejectsNonNumericId() throws Exception {
        mvc.perform(get("/api/tasks/not-a-number")).andExpect(status().isBadRequest());
    }
}
