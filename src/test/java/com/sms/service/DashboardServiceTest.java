package com.sms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sms.dto.dashboard.DashboardResponse;
import com.sms.dto.dashboard.TaskDto;
import com.sms.model.Course;
import com.sms.repository.TaskItemRepository;
import com.sms.repository.CourseRepository;

@SpringBootTest
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TaskItemRepository taskItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void shouldBuildDashboardSummary() {
        DashboardResponse response = dashboardService.getStudentDashboard("S-1001");

        assertNotNull(response);
        assertEquals("S-1001", response.getStudentId());
        assertFalse(response.getCourses().isEmpty());
        assertFalse(response.getTasks().isEmpty());
    }

    @Test
    void shouldMarkTaskCompleted() {
        Long taskId = taskItemRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        TaskDto dto = dashboardService.markTaskCompleted("S-1001", taskId);

        assertEquals(com.sms.model.TaskStatus.COMPLETED, dto.getStatus());
    }

    @Test
    void shouldCalculateDerivedProgress() {
        Course course = courseRepository.findAll().stream().findFirst().orElseThrow();
        double progress = dashboardService.calculateProgress("S-1001", course.getId());
        assertTrue(progress >= 0.0 && progress <= 100.0);
    }
}
