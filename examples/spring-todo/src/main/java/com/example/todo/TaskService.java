package com.example.todo;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
@Service
public class TaskService {
    public record Task(long id, String content, boolean done) {}
    // 此阶段只观察 Web 请求；程序重启后，内存中的任务会消失。
    private final ConcurrentHashMap<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    public Task create(String content) {
        Task task = new Task(sequence.incrementAndGet(), content.strip(), false);
        tasks.put(task.id(), task);
        return task;
    }
    public List<Task> list(boolean onlyOpen) {
        return tasks.values().stream().filter(t -> !onlyOpen || !t.done())
                .sorted(Comparator.comparingLong(Task::id)).toList();
    }
    public Task find(long id) {
        Task task = tasks.get(id);
        if (task == null) throw new TaskMissing();
        return task;
    }
    public Task finish(long id) {
        Task result = tasks.computeIfPresent(id, (key, old) -> new Task(key, old.content(), true));
        if (result == null) throw new TaskMissing();
        return result;
    }
    public void delete(long id) {
        if (tasks.remove(id) == null) throw new TaskMissing();
    }
    public static class TaskMissing extends RuntimeException {}
}
