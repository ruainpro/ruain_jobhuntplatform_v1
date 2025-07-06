package com.dao.rjobhunt.repository;

import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.models.ScraperRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScraperRequestRepository extends MongoRepository<ScraperRequest, String> {

    Optional<ScraperRequest> findByPublicId(UUID publicId);  // ✅ FIXED

	List<ScraperRequest> findByUserId(UUID userUuid);

	long deleteByUserId(String userId);
}