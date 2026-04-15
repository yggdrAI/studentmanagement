package com.sms.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.TaskItem;

@Repository
public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    List<TaskItem> findByCourseId(Long courseId);

    List<TaskItem> findByCourseIdIn(Collection<Long> courseIds);

    long countByCourseId(Long courseId);
}
