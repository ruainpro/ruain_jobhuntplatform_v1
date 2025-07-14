package com.dao.rjobhunt.repository;

import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.models.ScraperRequest;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScraperRequestRepository extends MongoRepository<ScraperRequest, String> {

    Optional<ScraperRequest> findByPublicId(UUID publicId);  //  FIXED

    List<ScraperRequest> findByUserId(UUID userUuid);

    long deleteByUserId(String userId);

    /** ✅ Count requests with autorun enabled */
    long countByEnableAutorunTrue();

    /** ✅ Count requests grouped by platformId */
    long countByPlatformId(String platformId);

    /** ✅ Find all requests by platformId */
    List<ScraperRequest> findAllByPlatformId(String platformId);

    /** ✅ Find by created date range */
    List<ScraperRequest> findByCreatedDateBetween(Date start, Date end);

    /** ✅ Get top queries */
    @Query(value = "{}", fields = "{ query : 1 }")
    List<ScraperRequest> findAllQueries();

    /** ✅ Count of scrapes by query */
    @Query(value = "{}", fields = "{ query : 1 }")
    List<ScraperRequest> findDistinctByQuery();

    /** ✅ Total number of distinct users who used scraper */
    @Query(value = "{}", fields = "{ userId : 1 }")
    List<ScraperRequest> findDistinctByUserId();

    /** ✅ Scraper activity by platform and time */
    @Query(value = "{}", fields = "{ platformId : 1, createdDate : 1 }")
    List<ScraperRequest> findScraperActivityDetails();

    /** ✅ Count by userId */
    long countByUserId(UUID userId);

    /** ✅ Fetch scraper requests with autorun enabled and created within a date */
    List<ScraperRequest> findByEnableAutorunTrueAndCreatedDateBetween(Date start, Date end);

	Long countByEnableAutorun(boolean b);
} 
