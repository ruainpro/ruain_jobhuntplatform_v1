package com.dao.rjobhunt.Controller.authentication;

import com.dao.rjobhunt.Service.ReportService;
import com.dao.rjobhunt.dto.*;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminReportController {

    @Autowired
    private final ReportService reportService;
    
    @Autowired
    private UserInfoRepository userInfoRepository;

    // ✅ 1. User Reports
    @GetMapping("/users/total")
    public ResponseEntity<Long> getTotalUsers() {
        return ResponseEntity.ok(reportService.getTotalRegisteredUsers());
    }

    @GetMapping("/users/status")
    public ResponseEntity<Map<String, Long>> getActiveVsInactiveUsers() {
        return ResponseEntity.ok(reportService.getActiveInactiveUsers());
    }

    @GetMapping("/users/growth")
    public List<UserGrowthDto> getUserGrowth(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
    	List<UserGrowthDto> userGrowthNew = reportService.aggregateUserGrowth(startDate, endDate);
        return userGrowthNew;
    }

    @GetMapping("/users/locations")
    public ResponseEntity<Map<String, Long>> getTopUserLocations() {
        return ResponseEntity.ok(reportService.getUserLocations());
    }

    @GetMapping("/users/notification-preferences")
    public ResponseEntity<Map<String, Long>> getNotificationPreferencesUsage() {
        return ResponseEntity.ok(reportService.getNotificationPreferencesUsage());
    }

    @GetMapping("/scrapers/top-queries")
    public ResponseEntity<Map<String, Long>> getTopScrapeQueries() {
        return ResponseEntity.ok(reportService.getTopScrapeQueries());
    }

    @GetMapping("/scrapers/trends")
    public ResponseEntity<List<TimeSeriesDto>> getScraperRunTrends() {
        return ResponseEntity.ok(reportService.getScraperRunTrends());
    }

    @GetMapping("/scrapers/autorun-usage")
    public ResponseEntity<Long> getAutoRunUsageCount() {
        return ResponseEntity.ok(reportService.getAutoRunUsageCount());
    }

    // ✅ 3. Job Notification Reports
    @GetMapping("/notifications/total")
    public ResponseEntity<Long> getTotalNotifiedJobs() {
        return ResponseEntity.ok(reportService.getTotalJobsNotified());
    }

    @GetMapping("/notifications/source-breakdown")
    public Map<String, Long> getNotificationBySource(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return reportService.getNotificationCountsBySource(startDate, endDate);
    }

    @GetMapping("/notifications/user-wise")
    public ResponseEntity<Map<String, Long>> getUserNotificationCounts() {
        return ResponseEntity.ok(reportService.getNotificationCountPerUser());
    }

    @GetMapping("/notifications/frequency")
    public ResponseEntity<List<TimeSeriesDto>> getNotificationTrends() {
        return ResponseEntity.ok(reportService.getNotificationTrendsOverTime());
    }

    @GetMapping("/notifications/type-ratio")
    public ResponseEntity<Map<String, Long>> getAutoVsManualNotificationRatio() {
        return ResponseEntity.ok(reportService.getAutoVsManualNotificationRatio());
    }

    @GetMapping("/platforms/preference-impact")
    public ResponseEntity<List<PreferenceImpactDto>> getPreferenceWeightImpact() {
        return ResponseEntity.ok(reportService.getPreferenceWeightImpact());
    }

    // ✅ 5. System Activity Reports
    @GetMapping("/activity/logins")
    public ResponseEntity<List<TimeSeriesDto>> getLoginActivity() {
        return ResponseEntity.ok(reportService.getLoginActivityOverTime());
    }

    @GetMapping("/activity/actions")
    public List<ActionStatDto> getActionStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
            
            
    ) {
    	List<ActionStatDto>  newactionDto= reportService.getActionHistorySummary(startDate, endDate);
        return newactionDto;
    }

    @GetMapping("/activity/top-users")
    public ResponseEntity<Map<String, Long>> getMostActiveUsers() {
        return ResponseEntity.ok(reportService.getMostActiveUsers());
    }

    @GetMapping("/activity/ip-distribution")
    public ResponseEntity<Map<String, Long>> getIpAddressReport() {
        return ResponseEntity.ok(reportService.getIpAccessDistribution());
    }

    @GetMapping("/activity/devices")
    public ResponseEntity<Map<String, Long>> getDeviceUsageReport() {
        return ResponseEntity.ok(reportService.getDeviceUsageDistribution());
    }

    @GetMapping("/users/export-csv")
    public void exportUsersToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=users.csv");

        List<User> users = userInfoRepository.findAll();
        PrintWriter writer = response.getWriter();
        writer.println("Email,Role,Status,CreatedAt");

        for (User user : users) {
            writer.printf("%s,%s,%s,%s\n",
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus().getStatusId(),
                user.getCreatedAt());
        }

        writer.flush();
        writer.close();
    }
}
