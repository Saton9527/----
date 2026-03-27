package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.*;
import com.acmtrain.backend.entity.TrainingTaskEntity;
import com.acmtrain.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrainingQueryServiceTest {

    @Mock
    private TrainingTaskRepository trainingTaskRepository;

    @Mock
    private RankingOverallRepository rankingOverallRepository;

    @Mock
    private PointLogRepository pointLogRepository;

    @Mock
    private TrendPointRepository trendPointRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private AlertLogRepository alertLogRepository;

    @Mock
    private StudentInfoRepository studentInfoRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private TrainingQueryService trainingQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testTasks() {
        TrainingTaskEntity task1 = new TrainingTaskEntity();
        task1.setId(1L);
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setDeadline(LocalDateTime.now());
        task1.setStatus("PUBLISHED");
        task1.setTotalProblems(10);
        task1.setCompletedProblems(5);

        List<TrainingTaskEntity> tasks = List.of(task1);
        Page<TrainingTaskEntity> taskPage = new PageImpl<>(tasks, PageRequest.of(0, 10), tasks.size());

        when(trainingTaskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);

        PageResponse<TaskResponse> result = trainingQueryService.tasks(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Task 1", result.content().get(0).title());
        verify(trainingTaskRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testCreateTask() {
        CreateTaskRequest request = new CreateTaskRequest(
                "New Task",
                "New Task Description",
                LocalDateTime.now().toString(),
                "PUBLISHED",
                20
        );

        TrainingTaskEntity savedTask = new TrainingTaskEntity();
        savedTask.setId(1L);
        savedTask.setTitle(request.title());
        savedTask.setDescription(request.description());
        savedTask.setDeadline(LocalDateTime.now());
        savedTask.setStatus(request.status());
        savedTask.setTotalProblems(request.totalProblems());
        savedTask.setCompletedProblems(0);

        when(trainingTaskRepository.save(any(TrainingTaskEntity.class))).thenReturn(savedTask);

        TaskResponse result = trainingQueryService.createTask(request);

        assertNotNull(result);
        assertEquals("New Task", result.title());
        verify(trainingTaskRepository, times(1)).save(any(TrainingTaskEntity.class));
    }

    @Test
    void testUpdateTaskStatus() {
        Long taskId = 1L;
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest("DONE");

        TrainingTaskEntity existingTask = new TrainingTaskEntity();
        existingTask.setId(taskId);
        existingTask.setTitle("Task 1");
        existingTask.setDeadline(LocalDateTime.now());
        existingTask.setStatus("PUBLISHED");

        TrainingTaskEntity updatedTask = new TrainingTaskEntity();
        updatedTask.setId(taskId);
        updatedTask.setTitle("Task 1");
        updatedTask.setDeadline(LocalDateTime.now());
        updatedTask.setStatus("DONE");

        when(trainingTaskRepository.findById(taskId)).thenReturn(java.util.Optional.of(existingTask));
        when(trainingTaskRepository.save(any(TrainingTaskEntity.class))).thenReturn(updatedTask);

        TaskResponse result = trainingQueryService.updateTaskStatus(taskId, request);

        assertNotNull(result);
        assertEquals("DONE", result.status());
        verify(trainingTaskRepository, times(1)).findById(taskId);
        verify(trainingTaskRepository, times(1)).save(any(TrainingTaskEntity.class));
    }

    @Test
    void testUpdateTaskStatus_TaskNotFound() {
        Long taskId = 1L;
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest("DONE");

        when(trainingTaskRepository.findById(taskId)).thenReturn(java.util.Optional.empty());

        assertThrows(ResponseStatusException.class, () -> trainingQueryService.updateTaskStatus(taskId, request));

        verify(trainingTaskRepository, times(1)).findById(taskId);
    }

    @Test
    void testUpdateTaskProgress() {
        Long taskId = 1L;
        UpdateTaskProgressRequest request = new UpdateTaskProgressRequest(10);

        TrainingTaskEntity existingTask = new TrainingTaskEntity();
        existingTask.setId(taskId);
        existingTask.setTitle("Task 1");
        existingTask.setDeadline(LocalDateTime.now());
        existingTask.setStatus("PUBLISHED");
        existingTask.setTotalProblems(10);
        existingTask.setCompletedProblems(5);

        TrainingTaskEntity updatedTask = new TrainingTaskEntity();
        updatedTask.setId(taskId);
        updatedTask.setTitle("Task 1");
        updatedTask.setDeadline(LocalDateTime.now());
        updatedTask.setStatus("DONE");
        updatedTask.setTotalProblems(10);
        updatedTask.setCompletedProblems(10);

        when(trainingTaskRepository.findById(taskId)).thenReturn(java.util.Optional.of(existingTask));
        when(trainingTaskRepository.save(any(TrainingTaskEntity.class))).thenReturn(updatedTask);

        TaskResponse result = trainingQueryService.updateTaskProgress(taskId, request);

        assertNotNull(result);
        assertEquals(10, result.completedProblems());
        assertEquals("DONE", result.status());
        verify(trainingTaskRepository, times(1)).findById(taskId);
        verify(trainingTaskRepository, times(1)).save(any(TrainingTaskEntity.class));
    }

    @Test
    void testUpdateTaskProgress_InvalidValue() {
        Long taskId = 1L;
        UpdateTaskProgressRequest request = new UpdateTaskProgressRequest(15);

        TrainingTaskEntity existingTask = new TrainingTaskEntity();
        existingTask.setId(taskId);
        existingTask.setTitle("Task 1");
        existingTask.setDeadline(LocalDateTime.now());
        existingTask.setStatus("PUBLISHED");
        existingTask.setTotalProblems(10);
        existingTask.setCompletedProblems(5);

        when(trainingTaskRepository.findById(taskId)).thenReturn(java.util.Optional.of(existingTask));

        assertThrows(ResponseStatusException.class, () -> trainingQueryService.updateTaskProgress(taskId, request));

        verify(trainingTaskRepository, times(1)).findById(taskId);
    }
}