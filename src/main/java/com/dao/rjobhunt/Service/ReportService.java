package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.dto.*;
import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.models.UserNotificationFeed;
import com.dao.rjobhunt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators.DateFromParts;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserInfoRepository userRepo;
    private final ScraperRequestRepository scraperRepo;
    private final UserNotificationFeedRepository feedRepo;
    private final PlatformRepository platformRepo;
    private final ActionHistoryRepository actionRepo;
    private final MongoTemplate mongoTemplate;

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate parseDateOrDefault(String dateStr, LocalDate fallback) {
        try {
            return (dateStr != null && !dateStr.isEmpty()) ?
                    LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }


public List<UserGrowthDto> aggregateUserGrowth(String startDateStr, String endDateStr) {
    LocalDate from = parseDateOrDefault(startDateStr, LocalDate.now().minusMonths(6));
    LocalDate to = parseDateOrDefault(endDateStr, LocalDate.now());
    Date fromDate = toDate(from);
    Date toDate = toDate(to.plusDays(1)); // Include full end date

    Aggregation agg = Aggregation.newAggregation(
        // Step 1: Filter users by createdAt
        Aggregation.match(Criteria.where("createdAt").gte(fromDate).lte(toDate)),

        // Step 2: Project year, month, and day parts
        Aggregation.project()
            .andExpression("year($createdAt)").as("year")
            .andExpression("month($createdAt)").as("month")
            .andExpression("dayOfMonth($createdAt)").as("day"),

        // Step 3: Group by year/month/day and count
        Aggregation.group("year", "month", "day").count().as("count"),

        // Step 4: Reconstruct date from parts using DateOperators
        Aggregation.project("count")
            .and(
                DateFromParts
                    .dateFromParts()
                    .year("$_id.year")
                    .month("$_id.month")
                    .day("$_id.day")
            ).as("date"),

        // Step 5: Format date as string
        Aggregation.project("count")
            .and(DateOperators.DateToString
                .dateOf("date")
                .toString("%Y-%m-%d")
            ).as("date"),

        Aggregation.sort(Sort.Direction.ASC, "date")
    );

    return mongoTemplate.aggregate(agg, "user", UserGrowthDto.class).getMappedResults();
}


    public Map<String, Long> getNotificationPreferencesUsage() {
        long email = userRepo.countByNotification_EmailEnabled(true);
        long sms = userRepo.countByNotification_SmsEnabled(true);
        long discord = userRepo.countByNotification_DiscordEnabled(true);
        return Map.of("Email", email, "SMS", sms, "Discord", discord);
    }


    public List<ActionStatDto> getActionHistorySummary(String startDateStr, String endDateStr) {
        LocalDate start = parseDateOrDefault(startDateStr, LocalDate.now().minusMonths(6));
        LocalDate end = parseDateOrDefault(endDateStr, LocalDate.now());
        Date fromDate = toDate(start);
        Date toDate = toDate(end.plusDays(1));

        Query query = new Query();
        query.addCriteria(Criteria.where("timestamp").gte(fromDate).lte(toDate));
        List<ActionHistory> actions = mongoTemplate.find(query, ActionHistory.class);

        Map<String, Long> typeCounts = new HashMap<>();

        for (ActionHistory action : actions) {
            String desc = Optional.ofNullable(action.getDescription()).orElse("").toLowerCase();
            String category = categorizeDescription(desc);
            typeCounts.put(category, typeCounts.getOrDefault(category, 0L) + 1);
        }

        return typeCounts.entrySet().stream()
                .map(entry -> new ActionStatDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String categorizeDescription(String desc) {
        if (desc.contains("login")) return "LOGIN";
        if (desc.contains("logout")) return "LOGOUT";
        if (desc.contains("register")) return "REGISTER";
        if (desc.contains("scrape") || desc.contains("scraper")) return "SCRAPER";
        if (desc.contains("report")) return "REPORT";
        if (desc.contains("preference")) return "PREFERENCE";
        if (desc.contains("job")) return "JOB";
        return "OTHER";
    }

    public Long getTotalRegisteredUsers() {
        return userRepo.count();
    }

    public Map<String, Long> getActiveInactiveUsers() {
        long active = userRepo.countByAccountStatus_StatusId(1);
        long inactive = userRepo.countByAccountStatus_StatusId(0);
        return Map.of("Active", active, "Inactive", inactive);
    }

    public List<UserGrowthDto> getUserGrowthReport(LocalDate startDate, LocalDate endDate) {
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        return userRepo.aggregateUserGrowth(from, to);
    }

    public List<UserGrowthDto> getUserGrowthOverTime() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.project()
                        .andExpression("{$dateToString: {format: '%Y-%m-%d', date: '$createdAt'}}").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        AggregationResults<UserGrowthDto> results = mongoTemplate.aggregate(agg, "user", UserGrowthDto.class);
        return results.getMappedResults();
    }

    public Map<String, Long> getUserLocations() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("address").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(5)
        );
        List<GenericCountDto> topLocations = mongoTemplate.aggregate(agg, "user", GenericCountDto.class).getMappedResults();
        return topLocations.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }

    public Map<String, Long> getTopScrapeQueries() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("query").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(10)
        );
        List<GenericCountDto> queries = mongoTemplate.aggregate(agg, "scraperRequest", GenericCountDto.class).getMappedResults();
        return queries.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }

    public List<TimeSeriesDto> getScraperRunTrends() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.project()
                        .andExpression("{$dateToString: {format: '%Y-%m-%d', date: '$createdDate'}}").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        return mongoTemplate.aggregate(agg, "scraperRequest", TimeSeriesDto.class).getMappedResults();
    }

    public Long getAutoRunUsageCount() {
        return scraperRepo.countByEnableAutorun(true);
    }

    public Long getTotalJobsNotified() {
        return feedRepo.count();
    }

    public Map<String, Long> getNotificationCountsBySource(String startDateStr, String endDateStr) {
        LocalDate start = parseDateOrDefault(startDateStr, LocalDate.now().minusMonths(6));
        LocalDate end = parseDateOrDefault(endDateStr, LocalDate.now());

        Criteria criteria = Criteria.where("notifiedAt").gte(start).lte(end);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.group("jobSource").count().as("count")
        );
        List<GenericCountDto> result = mongoTemplate.aggregate(agg, "user_notification_feeds", GenericCountDto.class).getMappedResults();
        return result.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }

    public Map<String, Long> getNotificationCountPerUser() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("userId").count().as("notificationCount"),
                Aggregation.limit(10)
        );
        List<GenericCountDto> result = mongoTemplate.aggregate(agg, "user_notification_feeds", GenericCountDto.class).getMappedResults();
        return result.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }

    public List<TimeSeriesDto> getNotificationTrendsOverTime() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.project()
                        .andExpression("{$dateToString: {format: '%Y-%m-%d', date: '$notifiedAt'}}").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        return mongoTemplate.aggregate(agg, "user_notification_feeds", TimeSeriesDto.class).getMappedResults();
    }

    public Map<String, Long> getAutoVsManualNotificationRatio() {
        long auto = feedRepo.countByLimited(false);
        long manual = feedRepo.countByLimited(true);
        return Map.of("Auto", auto, "Manual", manual);
    }

    public List<PreferenceImpactDto> getPreferenceWeightImpact() {
        List<Platform> platforms = platformRepo.findAll();

        Map<Integer, Long> scrapeCountsByWeight = platforms.stream()
            .collect(Collectors.groupingBy(
                Platform::getPreferenceWeight,
                Collectors.summingLong(p -> {
                    String platformId = p.getPublicId().toString();
                    return scraperRepo.countByPlatformId(platformId);
                })
            ));

        return platforms.stream()
            .collect(Collectors.groupingBy(Platform::getName))
            .entrySet()
            .stream()
            .map(entry -> {
                Platform platform = entry.getValue().get(0);
                int weight = platform.getPreferenceWeight();
                long count = scrapeCountsByWeight.getOrDefault(weight, 0L);
                return new PreferenceImpactDto(platform.getName(), weight, count);
            })
            .sorted(Comparator.comparing(PreferenceImpactDto::getPreferenceWeight).reversed())
            .toList();
    }

    public List<TimeSeriesDto> getLoginActivityOverTime() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("actionEntity").is("USER").and("actionType").is("LOGIN")),
                Aggregation.project()
                        .andExpression("{$dateToString: {format: '%Y-%m-%d', date: '$timestamp'}}").as("date"),
                Aggregation.group("date").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        return mongoTemplate.aggregate(agg, "actionHistory", TimeSeriesDto.class).getMappedResults();
    }

    public Map<String, Long> getMostActiveUsers() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("userId").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(10)
        );
        List<GenericCountDto> result = mongoTemplate.aggregate(agg, "actionHistory", GenericCountDto.class).getMappedResults();
        return result.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }

    public Map<String, Long> getIpAccessDistribution() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("ipAddress").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(10)
        );
        List<GenericCountDto> results = mongoTemplate.aggregate(agg, "actionHistory", GenericCountDto.class).getMappedResults();
        return results.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }

    public Map<String, Long> getDeviceUsageDistribution() {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("deviceInfo").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(10)
        );
        List<GenericCountDto> results = mongoTemplate.aggregate(agg, "actionHistory", GenericCountDto.class).getMappedResults();
        return results.stream().collect(Collectors.toMap(GenericCountDto::getId, GenericCountDto::getCount));
    }
}
