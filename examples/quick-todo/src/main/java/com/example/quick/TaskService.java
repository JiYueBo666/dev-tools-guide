package com.example.quick;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
@Service
public class TaskService {
    public record TaskView(long id, String content, boolean done) {
        static TaskView of(TaskEntity t) { return new TaskView(t.getId(), t.getContent(), t.isDone()); }
    }
    public record TaskPage(List<TaskView> items, long total, int page, int size) {}
    public static class Missing extends RuntimeException {}
    private final TaskRepository repository;
    public TaskService(TaskRepository repository) { this.repository = repository; }
    @Transactional
    public TaskView create(String content) { return TaskView.of(repository.save(new TaskEntity(content.strip()))); }
    @Transactional(readOnly=true)
    public TaskView find(long id) { return TaskView.of(required(id)); }
    @Transactional(readOnly=true)
    public TaskPage list(int page, int size, Boolean done) {
        // 对外页码从 1 开始，排序字段固定，避免直接接受任意列名。
        var request = PageRequest.of(page - 1, size, Sort.by("id").descending());
        var result = done == null ? repository.findByDeletedFalse(request)
                : repository.findByDeletedFalseAndDone(done, request);
        return new TaskPage(result.map(TaskView::of).getContent(), result.getTotalElements(), page, size);
    }
    @Transactional
    public TaskView changeDone(long id, boolean done) {
        var task = required(id);
        task.setDone(done); // false 也会写回；方法成功返回后提交事务。
        return TaskView.of(task);
    }
    @Transactional
    public void delete(long id) { required(id).markDeleted(); }
    @Transactional
    public List<TaskView> importTasks(List<String> contents) {
        var entities = contents.stream().map(String::strip).map(TaskEntity::new).toList();
        return repository.saveAll(entities).stream().map(TaskView::of).toList();
    }
    private TaskEntity required(long id) {
        return repository.findByIdAndDeletedFalse(id).orElseThrow(Missing::new);
    }
}
