package com.example.todo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    public record CreateTask(@NotBlank(message="请输入任务内容") @Size(max=200, message="任务最多200个字符") String content) {}
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }
    @GetMapping
    public List<TaskService.Task> list(@RequestParam(defaultValue="false") boolean onlyOpen) {
        return service.list(onlyOpen);
    }
    @GetMapping("/{id}")
    public TaskService.Task find(@PathVariable long id) { return service.find(id); }
    @PostMapping
    public ResponseEntity<TaskService.Task> create(@Valid @RequestBody CreateTask request) {
        var task = service.create(request.content());
        return ResponseEntity.created(URI.create("/api/tasks/" + task.id())).body(task);
    }
    @PatchMapping("/{id}/done")
    public TaskService.Task finish(@PathVariable long id) { return service.finish(id); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
