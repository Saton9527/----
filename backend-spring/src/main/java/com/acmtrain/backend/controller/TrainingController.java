package com.acmtrain.backend.controller;

import com.acmtrain.backend.dto.*;
import com.acmtrain.backend.service.StudentImportService;
import com.acmtrain.backend.service.TrainingQueryService;
import com.acmtrain.backend.service.OjSyncJobService;
import com.acmtrain.backend.service.OjSyncService;
import com.acmtrain.backend.service.ProfileContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TrainingController {

    private final TrainingQueryService trainingQueryService;
    private final StudentImportService studentImportService;
    private final OjSyncJobService ojSyncJobService;
    private final OjSyncService ojSyncService;
    private final ProfileContactService profileContactService;

    public TrainingController(
            TrainingQueryService trainingQueryService,
            StudentImportService studentImportService,
            OjSyncJobService ojSyncJobService,
            OjSyncService ojSyncService,
            ProfileContactService profileContactService
    ) {
        this.trainingQueryService = trainingQueryService;
        this.studentImportService = studentImportService;
        this.ojSyncJobService = ojSyncJobService;
        this.ojSyncService = ojSyncService;
        this.profileContactService = profileContactService;
    }

    @GetMapping("/tasks")
    public PageResponse<TaskResponse> tasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingQueryService.tasks(status, page, size);
    }

    @PostMapping("/tasks")
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return trainingQueryService.createTask(request);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse updateTaskStatus(@PathVariable Long id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        return trainingQueryService.updateTaskStatus(id, request);
    }

    @PatchMapping("/tasks/{id}/progress")
    public TaskResponse updateTaskProgress(@PathVariable Long id, @Valid @RequestBody UpdateTaskProgressRequest request) {
        return trainingQueryService.updateTaskProgress(id, request);
    }

    @GetMapping("/rankings/overall")
    public PageResponse<RankingResponse> rankings(
            @RequestParam(defaultValue = "TOTAL_POINTS") String metric,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingQueryService.rankings(metric, page, size);
    }

    @GetMapping("/points/me/logs")
    public PageResponse<PointLogResponse> points(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingQueryService.points(userId, page, size);
    }

    @GetMapping("/dashboard/me/trend")
    public List<TrendPointResponse> trend(@RequestAttribute("userId") Long userId) {
        return trainingQueryService.trend(userId);
    }

    @GetMapping("/dashboard/me/analytics")
    public DashboardAnalyticsResponse dashboardAnalytics(@RequestAttribute("userId") Long userId) {
        return trainingQueryService.dashboardAnalytics(userId);
    }

    @GetMapping("/profile/me")
    public MyProfileResponse myProfile(@RequestAttribute("userId") Long userId) {
        return trainingQueryService.myProfile(userId);
    }

    @PutMapping("/profile/me/platform-binding")
    public MyProfileResponse updatePlatformBinding(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdatePlatformBindingRequest request
    ) {
        return trainingQueryService.updatePlatformBinding(userId, request);
    }

    @PostMapping("/profile/me/sync-oj")
    public MyProfileResponse syncMyOjProfile(@RequestAttribute("userId") Long userId) {
        return ojSyncService.syncMyProfile(userId);
    }

    @PostMapping("/profile/me/sync-oj/jobs")
    public MyProfileSyncJobResponse startMyOjSyncJob(@RequestAttribute("userId") Long userId) {
        return ojSyncJobService.startMyProfileSync(userId);
    }

    @GetMapping("/profile/me/sync-oj/jobs/{jobId}")
    public MyProfileSyncJobResponse myOjSyncJob(
            @RequestAttribute("userId") Long userId,
            @PathVariable String jobId
    ) {
        return ojSyncJobService.getMyProfileSync(userId, jobId);
    }

    @PostMapping("/profile/me/import-atc-submissions")
    public MyProfileResponse importMyAtCoderSubmissions(
            @RequestAttribute("userId") Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ojSyncService.importMyAtCoderSubmissions(userId, file);
    }

    @GetMapping("/profile/me/contact-email")
    public ContactEmailResponse myContactEmail(@RequestAttribute("userId") Long userId) {
        return profileContactService.getMyContactEmail(userId);
    }

    @PutMapping("/profile/me/contact-email")
    public ContactEmailResponse updateMyContactEmail(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateContactEmailRequest request
    ) {
        return profileContactService.updateMyContactEmail(userId, request);
    }

    @GetMapping("/profile/me/contest-history")
    public List<OjContestHistoryResponse> myContestHistory(@RequestAttribute("userId") Long userId) {
        return ojSyncService.getMyContestHistory(userId);
    }

    @GetMapping("/recommendations/me")
    public PageResponse<RecommendationResponse> recommendations(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingQueryService.recommendations(userId, page, size);
    }

    @GetMapping("/problems")
    public PageResponse<ProblemCatalogResponse> problems(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Boolean solved,
            @RequestParam(required = false) Boolean recommended,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return trainingQueryService.problems(userId, keyword, minRating, maxRating, solved, recommended, page, size);
    }

    @GetMapping("/alerts")
    public PageResponse<AlertResponse> alerts(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingQueryService.alerts(userId, page, size);
    }

    @GetMapping("/alerts/me")
    public PageResponse<AlertResponse> myAlerts(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return trainingQueryService.myAlerts(userId, page, size);
    }

    @PatchMapping("/alerts/{id}/feedback")
    public AlertResponse updateMyAlertFeedback(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertFeedbackRequest request
    ) {
        return trainingQueryService.updateMyAlertFeedback(userId, id, request);
    }

    @GetMapping("/students")
    public PageResponse<StudentResponse> students(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingQueryService.students(page, size);
    }

    @PostMapping("/students")
    public StudentResponse createStudent(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateStudentRequest request
    ) {
        return trainingQueryService.createStudent(userId, request);
    }

    @PutMapping("/students/{id}")
    public StudentResponse updateStudent(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request
    ) {
        return trainingQueryService.updateStudent(userId, id, request);
    }

    @PostMapping("/students/{id}/sync-oj")
    public StudentResponse syncStudentOj(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id
    ) {
        return ojSyncService.syncStudentById(userId, id);
    }

    @PostMapping("/students/{id}/sync-oj/jobs")
    public StudentSyncJobResponse startStudentOjSyncJob(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id
    ) {
        return ojSyncJobService.startStudentSync(userId, id);
    }

    @GetMapping("/students/{id}/sync-oj/jobs/{jobId}")
    public StudentSyncJobResponse studentOjSyncJob(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @PathVariable String jobId
    ) {
        return ojSyncJobService.getStudentSync(userId, id, jobId);
    }

    @PostMapping("/students/{id}/import-atc-submissions")
    public StudentResponse importStudentAtCoderSubmissions(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return ojSyncService.importStudentAtCoderSubmissions(userId, id, file);
    }

    @PostMapping("/students/import")
    public StudentImportResultResponse importStudents(
            @RequestAttribute("userId") Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return studentImportService.importStudents(userId, file);
    }

    @GetMapping("/students/import/template")
    public ResponseEntity<byte[]> downloadStudentImportTemplate(@RequestAttribute("userId") Long userId) {
        byte[] template = studentImportService.downloadTemplate(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-import-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }
}
