package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.repository.PlatformRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlatformService {

    private final PlatformRepository platformRepo;

    public ResponseEntity<ApiResponse<Map<String, List<Platform>>>> getAllPlatforms(String userId, boolean isAdmin) {
    	
    	
        List<Platform> all = platformRepo.findAll();

        if (isAdmin) {
            return ResponseEntity.ok(ApiResponse.success("All platforms retrieved", Map.of("all", all)));
        }

        List<Platform> common = all.stream()
                .filter(p -> !"custom".equalsIgnoreCase(p.getType()))
                .toList();

        List<Platform> custom = all.stream()
                .filter(p -> "custom".equalsIgnoreCase(p.getType()) && userId.equals(p.getCreatedByUserId()))
                .toList();

        Map<String, List<Platform>> response = new HashMap<>();
        response.put("common", common);
        response.put("custom", custom);
        
        return ResponseEntity.ok(ApiResponse.success("Platforms retrieved for user", response));
    }

    public ResponseEntity<ApiResponse<Platform>> createPlatform(Platform platform, String userId, boolean isAdmin) {
        platform.setPublicId(UUID.randomUUID());
        platform.setCreatedByUserId(userId);
        platform.setCreatedDate(LocalDateTime.now());

        // -----------------------------
        // Validate duplicate (type+parserType+url) for non-custom types
        // -----------------------------
        if (!"custom".equalsIgnoreCase(platform.getType())) {
            boolean exists = platformRepo.existsByTypeIgnoreCaseAndParserTypeAndUrlIgnoreCase(
                platform.getType(), platform.getParserType(), platform.getUrl()
            );
            if (exists) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Platform with same type, parserType, and URL already exists."));
            }
        }

        // -----------------------------
        // Validate name or URL duplicates globally (you had this already)
        // -----------------------------
        if (platformRepo.existsByNameIgnoreCase(platform.getName())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Platform with the same name already exists."));
        }
        if (platformRepo.existsByUrl(platform.getUrl())) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Platform with the same URL already exists."));
        }

        if (platform.getIsActive() != null) {
            platform.setIsActive(platform.getIsActive());
        }

        platformRepo.save(platform);
    
        return ResponseEntity.ok(ApiResponse.success("Platform created successfully", platform));
    }

    public ResponseEntity<ApiResponse<Platform>> updatePlatform(String publicId, Platform updates, String userId, boolean isAdmin) {
        Optional<Platform> existingOpt = platformRepo.findByPublicId(UUID.fromString(publicId));

        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Platform existing = existingOpt.get();

        if (!isAdmin && !userId.equals(existing.getCreatedByUserId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized to update this platform."));
        }

        if (updates.getName() != null && !updates.getName().isBlank()) existing.setName(updates.getName());
        if (updates.getUrl() != null && !updates.getUrl().isBlank()) existing.setUrl(updates.getUrl());
        if (updates.getUrlTemplate() != null && !updates.getUrlTemplate().isBlank()) existing.setUrlTemplate(updates.getUrlTemplate());
        if (updates.getType() != null && !updates.getType().isBlank()) existing.setType(updates.getType());
        
        if (updates.getParserType() != null) {
            existing.setParserType(updates.getParserType());
        }
        
        if (updates.getScraperStatus() != null && !updates.getScraperStatus().isBlank()) existing.setScraperStatus(updates.getScraperStatus());
        if (updates.getPreferenceWeight() != 0) existing.setPreferenceWeight(updates.getPreferenceWeight());
        if (updates.getNotes() != null && !updates.getNotes().isBlank()) existing.setNotes(updates.getNotes());
        if (updates.getSelectors() != null && !updates.getSelectors().isEmpty()) existing.setSelectors(updates.getSelectors());
        if (updates.getDetectionYaml() != null && !updates.getDetectionYaml().isBlank()) existing.setDetectionYaml(updates.getDetectionYaml());

        if (updates.getIsActive() != null) {
            existing.setIsActive(updates.getIsActive());
        }
        
        platformRepo.save(existing);
        
        return ResponseEntity.ok(ApiResponse.success("Platform updated successfully", existing));
    }

    public ResponseEntity<ApiResponse<String>> deletePlatform(String publicId, String userId, boolean isAdmin) {
        Optional<Platform> existingOpt = platformRepo.findByPublicId(UUID.fromString(publicId));

        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Platform existing = existingOpt.get();

        if (!isAdmin && !userId.equals(existing.getCreatedByUserId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized to delete this platform."));
        }

        platformRepo.delete(existing);

        return ResponseEntity.ok(ApiResponse.success("Platform deleted successfully", null));
    }
}