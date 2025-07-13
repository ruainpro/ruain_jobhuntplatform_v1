package com.dao.rjobhunt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dao.rjobhunt.models.UserNotificationFeed;

public interface UserNotificationFeedRepository extends MongoRepository<UserNotificationFeed, String> {
    List<UserNotificationFeed> findByUserId(UUID userId);

    boolean existsByUserIdAndRequestId(UUID userId, String requestId);
}