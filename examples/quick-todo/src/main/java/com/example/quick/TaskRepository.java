package com.example.quick;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    Page<TaskEntity> findByDeletedFalse(Pageable pageable);
    Page<TaskEntity> findByDeletedFalseAndDone(boolean done, Pageable pageable);
    Optional<TaskEntity> findByIdAndDeletedFalse(long id);
}
