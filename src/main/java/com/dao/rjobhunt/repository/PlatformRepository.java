package com.dao.rjobhunt.repository;

import com.dao.rjobhunt.models.Platform;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformRepository extends MongoRepository<Platform, String> {
    boolean existsByTypeIgnoreCase(String type);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByUrl(String url);

    Optional<Platform> findByPublicId(UUID publicId);

	Optional<Platform> findByNameIgnoreCase(String platform);
}