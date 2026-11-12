package com.example.quick;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    public record Create(@NotBlank @Size(max=200) String content) {}
    public record Change(@NotNull Boolean done) {}
    public record Import(@NotEmpty @Size(max=100) List<@NotBlank @Size(max=200) String> contents) {}
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<TaskService.TaskView> create(@Valid @RequestBody Create input) {
        var result = service.create(input.content());
        return ResponseEntity.created(URI.create("/api/tasks/" + result.id())).body(result);
    }
    @GetMapping("/{id}")
    public TaskService.TaskView find(@PathVariable @Positive long id) { return service.find(id); }
    @GetMapping
    public TaskService.TaskPage list(@RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) Boolean done) {
        return service.list(page, size, done);
    }
    @PatchMapping("/{id}")
    public TaskService.TaskView change(@PathVariable @Positive long id, @Valid @RequestBody Change input) {
        return service.changeDone(id, input.done());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/imports")
    public List<TaskService.TaskView> importTasks(@Valid @RequestBody Import input) {
        return service.importTasks(input.contents());
    }
}
