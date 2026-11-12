package com.example.quick;
import jakarta.persistence.*;
@Entity
@Table(name="quick_tasks")
public class TaskEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=200)
    private String content;
    private boolean done;
    private boolean deleted;
    protected TaskEntity() {}
    public TaskEntity(String content) { this.content = content; }
    public Long getId() { return id; }
    public String getContent() { return content; }
    public boolean isDone() { return done; }
    public boolean isDeleted() { return deleted; }
    public void setDone(boolean done) { this.done = done; }
    public void markDeleted() { deleted = true; }
}
