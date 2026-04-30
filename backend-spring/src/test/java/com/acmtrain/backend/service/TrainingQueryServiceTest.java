package com.acmtrain.backend.service;

import com.acmtrain.backend.dto.*;
import com.acmtrain.backend.entity.OjSolvedProblemEntity;
import com.acmtrain.backend.entity.StudentInfoEntity;
import com.acmtrain.backend.entity.TrainingTaskEntity;
import com.acmtrain.backend.entity.UserAccountEntity;
import com.acmtrain.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Mock
    private OjSolvedProblemRepository ojSolvedProblemRepository;

    @Mock
    private CodeforcesCatalogService codeforcesCatalogService;

    private TrainingQueryService trainingQueryService;
    private final OjAccountValidationService ojAccountValidationService = new OjAccountValidationService() {
        @Override
        public void validateCodeforcesHandle(String handle) {
        }

        @Override
        public void validateAtCoderHandle(String handle) {
        }
    };

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        trainingQueryService = new TrainingQueryService(
                trainingTaskRepository,
                rankingOverallRepository,
                pointLogRepository,
                trendPointRepository,
                recommendationRepository,
                alertLogRepository,
                studentInfoRepository,
                userAccountRepository,
                ojSolvedProblemRepository,
                codeforcesCatalogService,
                ojAccountValidationService
        );
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

    @Test
    void testCreateStudent() {
        UserAccountEntity coach = new UserAccountEntity();
        coach.setId(99L);
        coach.setRole("coach");

        UserAccountEntity savedUser = new UserAccountEntity();
        savedUser.setId(10L);
        savedUser.setUsername("student10");
        savedUser.setRealName("学生十号");
        savedUser.setRole("student");

        StudentInfoEntity savedStudent = new StudentInfoEntity();
        savedStudent.setId(8L);
        savedStudent.setUserId(10L);
        savedStudent.setRealName("学生十号");
        savedStudent.setGrade("2023");
        savedStudent.setMajor("计算机科学与技术");
        savedStudent.setCfHandle("student10_cf");
        savedStudent.setAtcHandle("student10_atc");
        savedStudent.setCfRating(1600);
        savedStudent.setAtcRating(1450);
        savedStudent.setSolvedCount(120);
        savedStudent.setTotalPoints(BigDecimal.valueOf(220.0));

        when(userAccountRepository.findById(99L)).thenReturn(java.util.Optional.of(coach));
        when(userAccountRepository.findByUsername("student10")).thenReturn(java.util.Optional.empty());
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenReturn(savedUser);
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenReturn(savedStudent);

        StudentResponse result = trainingQueryService.createStudent(99L, new CreateStudentRequest(
                "student10",
                "123456",
                "学生十号",
                "2023",
                "计算机科学与技术",
                "student10_cf",
                "student10_atc",
                1600,
                1450,
                120,
                BigDecimal.valueOf(220.0)
        ));

        assertNotNull(result);
        assertEquals("student10", result.username());
        assertEquals("学生十号", result.realName());
        verify(userAccountRepository).save(any(UserAccountEntity.class));
        verify(studentInfoRepository).save(any(StudentInfoEntity.class));
    }

    @Test
    void testUpdateStudent() {
        UserAccountEntity coach = new UserAccountEntity();
        coach.setId(99L);
        coach.setRole("coach");

        StudentInfoEntity existingStudent = new StudentInfoEntity();
        existingStudent.setId(8L);
        existingStudent.setUserId(10L);
        existingStudent.setRealName("旧姓名");
        existingStudent.setGrade("2022");
        existingStudent.setMajor("软件工程");
        existingStudent.setCfHandle("old_cf");
        existingStudent.setCfRating(1200);
        existingStudent.setAtcRating(1100);
        existingStudent.setSolvedCount(50);
        existingStudent.setTotalPoints(BigDecimal.valueOf(80.0));

        UserAccountEntity existingUser = new UserAccountEntity();
        existingUser.setId(10L);
        existingUser.setUsername("student10");
        existingUser.setPassword("oldpass");
        existingUser.setRealName("旧姓名");
        existingUser.setRole("student");

        UserAccountEntity savedUser = new UserAccountEntity();
        savedUser.setId(10L);
        savedUser.setUsername("student10new");
        savedUser.setPassword("oldpass");
        savedUser.setRealName("新姓名");
        savedUser.setRole("student");

        StudentInfoEntity savedStudent = new StudentInfoEntity();
        savedStudent.setId(8L);
        savedStudent.setUserId(10L);
        savedStudent.setRealName("新姓名");
        savedStudent.setGrade("2023");
        savedStudent.setMajor("计算机科学与技术");
        savedStudent.setCfHandle("new_cf");
        savedStudent.setAtcHandle("new_atc");
        savedStudent.setCfRating(1650);
        savedStudent.setAtcRating(1500);
        savedStudent.setSolvedCount(180);
        savedStudent.setTotalPoints(BigDecimal.valueOf(260.0));

        when(userAccountRepository.findById(99L)).thenReturn(java.util.Optional.of(coach));
        when(studentInfoRepository.findById(8L)).thenReturn(java.util.Optional.of(existingStudent));
        when(userAccountRepository.findById(10L)).thenReturn(java.util.Optional.of(existingUser));
        when(userAccountRepository.findByUsername("student10new")).thenReturn(java.util.Optional.empty());
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenReturn(savedUser);
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenReturn(savedStudent);

        StudentResponse result = trainingQueryService.updateStudent(99L, 8L, new UpdateStudentRequest(
                "student10new",
                "",
                "新姓名",
                "2023",
                "计算机科学与技术",
                "new_cf",
                "new_atc",
                1650,
                1500,
                180,
                BigDecimal.valueOf(260.0)
        ));

        assertNotNull(result);
        assertEquals("student10new", result.username());
        assertEquals("新姓名", result.realName());
        verify(userAccountRepository).save(any(UserAccountEntity.class));
        verify(studentInfoRepository).save(any(StudentInfoEntity.class));
    }

    @Test
    void testTrendUsesRealSolvedProblemsForStudent() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(2L);
        user.setRole("student");

        LocalDate today = LocalDate.now();
        OjSolvedProblemEntity first = new OjSolvedProblemEntity();
        first.setAcceptedAt(today.minusDays(2).atTime(10, 0));
        OjSolvedProblemEntity second = new OjSolvedProblemEntity();
        second.setAcceptedAt(today.minusDays(2).atTime(21, 0));
        OjSolvedProblemEntity third = new OjSolvedProblemEntity();
        third.setAcceptedAt(today.minusDays(5).atTime(8, 30));

        when(userAccountRepository.findById(2L)).thenReturn(java.util.Optional.of(user));
        when(ojSolvedProblemRepository.findAllByUserIdAndAcceptedAtBetweenOrderByAcceptedAtAsc(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second, third));

        List<TrendPointResponse> result = trainingQueryService.trend(2L);

        assertEquals(7, result.size());
        assertEquals(2, result.stream().filter(item -> item.date().equals(today.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")))).findFirst().orElseThrow().solved());
        assertEquals(1, result.stream().filter(item -> item.date().equals(today.minusDays(5).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")))).findFirst().orElseThrow().solved());
    }

    @Test
    void testUpdatePlatformBindingAllowsUnbind() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(2L);
        user.setUsername("student01");
        user.setRealName("演示学生A");
        user.setRole("student");

        StudentInfoEntity student = new StudentInfoEntity();
        student.setId(1L);
        student.setUserId(2L);
        student.setRealName("演示学生A");
        student.setGrade("2023");
        student.setMajor("计算机科学与技术");
        student.setCfHandle("tourist");
        student.setAtcHandle("tourist");
        student.setCfSyncedHandle("tourist");
        student.setAtcSyncedHandle("tourist");
        student.setCfLastSubmissionEpochSecond(123456789L);
        student.setCfRating(3755);
        student.setAtcRating(3797);
        student.setSolvedCount(161);
        student.setTotalPoints(BigDecimal.valueOf(248.0));

        when(studentInfoRepository.findByUserId(2L)).thenReturn(java.util.Optional.of(student));
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.findById(2L)).thenReturn(java.util.Optional.of(user));

        MyProfileResponse result = trainingQueryService.updatePlatformBinding(2L, new UpdatePlatformBindingRequest(null, null));

        assertNotNull(result);
        assertNull(result.cfHandle());
        assertNull(result.atcHandle());
        assertEquals(0, result.cfRating());
        assertEquals(0, result.atcRating());
        assertEquals(0, result.solvedCount());
        assertEquals(BigDecimal.ZERO.setScale(1), result.totalPoints());
        verify(studentInfoRepository).save(any(StudentInfoEntity.class));
    }

    @Test
    void testUpdatePlatformBindingClearsStaleSyncStateWhenHandleChanges() {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(3L);
        user.setUsername("student02");
        user.setRealName("演示学生B");
        user.setRole("student");

        StudentInfoEntity student = new StudentInfoEntity();
        student.setId(2L);
        student.setUserId(3L);
        student.setRealName("演示学生B");
        student.setGrade("2023");
        student.setMajor("软件工程");
        student.setCfHandle("tourist");
        student.setAtcHandle("Benq");
        student.setCfSyncedHandle("tourist");
        student.setAtcSyncedHandle("Benq");
        student.setCfLastSubmissionEpochSecond(987654321L);
        student.setCfRating(3755);
        student.setAtcRating(3658);
        student.setSolvedCount(3883);
        student.setTotalPoints(BigDecimal.valueOf(2446.0));

        when(studentInfoRepository.findByUserId(3L)).thenReturn(java.util.Optional.of(student));
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountRepository.findById(3L)).thenReturn(java.util.Optional.of(user));

        MyProfileResponse result = trainingQueryService.updatePlatformBinding(3L, new UpdatePlatformBindingRequest("Benq", "Benq"));

        assertNotNull(result);
        assertEquals("Benq", result.cfHandle());
        assertEquals("Benq", result.atcHandle());
        assertEquals(0, result.cfRating());
        assertEquals(3658, result.atcRating());
        assertEquals(0, result.solvedCount());
        assertEquals(BigDecimal.ZERO.setScale(1), result.totalPoints());
        assertNull(student.getCfSyncedHandle());
        assertEquals("Benq", student.getAtcSyncedHandle());
        assertNull(student.getCfLastSubmissionEpochSecond());
        verify(studentInfoRepository).save(any(StudentInfoEntity.class));
    }

    @Test
    void testRecommendationsKeepBalancedBucketsForLowRatingStudents() {
        StudentInfoEntity student = new StudentInfoEntity();
        student.setId(1L);
        student.setUserId(2L);
        student.setRealName("演示学生A");
        student.setCfRating(800);
        student.setAtcRating(0);
        student.setSolvedCount(0);

        List<CodeforcesCatalogService.CatalogProblem> catalog = List.of(
                catalogProblem("CF 1002A1", "Generate superposition of all basis states", 800, "*special"),
                catalogProblem("CF 1003A", "Polycarp's Pockets", 800, "implementation"),
                catalogProblem("CF 1005A", "Tanya and Stairways", 800, "implementation"),
                catalogProblem("CF 1004A", "Sonya and Hotels", 900, "implementation"),
                catalogProblem("CF 1008B", "Turn the Rectangles", 1000, "greedy"),
                catalogProblem("CF 1001A", "Generate plus state or minus state", 1100, "*special"),
                catalogProblem("CF 1006A", "Adjacent Replacements", 1200, "implementation"),
                catalogProblem("CF 1000C", "Covered Points Count", 1300, "sortings")
        );

        when(studentInfoRepository.findByUserId(2L)).thenReturn(java.util.Optional.of(student));
        when(ojSolvedProblemRepository.findAllByUserIdOrderByAcceptedAtDesc(2L)).thenReturn(List.of());
        when(codeforcesCatalogService.loadProblems()).thenReturn(catalog);

        PageResponse<RecommendationResponse> result = trainingQueryService.recommendations(2L, 0, 12);

        assertNotNull(result);
        assertEquals(6, result.content().size());
        assertEquals(2, result.content().stream().filter(item -> "WARMUP".equals(item.level())).count());
        assertEquals(2, result.content().stream().filter(item -> "CORE".equals(item.level())).count());
        assertEquals(2, result.content().stream().filter(item -> "CHALLENGE".equals(item.level())).count());

        Map<String, Integer> expectedRatings = Map.of(
                "CF 1002A1", 800,
                "CF 1003A", 800,
                "CF 1005A", 800,
                "CF 1004A", 900,
                "CF 1008B", 1000,
                "CF 1001A", 1100,
                "CF 1006A", 1200,
                "CF 1000C", 1300
        );
        result.content().forEach(item -> assertEquals(expectedRatings.get(item.problemCode()), item.suggestedRating()));
    }

    private CodeforcesCatalogService.CatalogProblem catalogProblem(String code, String title, int rating, String tag) {
        return new CodeforcesCatalogService.CatalogProblem(
                code,
                title,
                rating,
                tag,
                List.of(tag),
                "https://codeforces.com/problemset/problem/demo"
        );
    }
}
