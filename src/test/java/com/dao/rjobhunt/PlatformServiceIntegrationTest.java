package com.dao.rjobhunt;


import com.dao.rjobhunt.Service.PlatformService;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.ParserType;
import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.repository.PlatformRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(PlatformService.class) // load PlatformService bean in Spring context
class PlatformServiceIntegrationTest {

    @Autowired
    private PlatformService platformService;

    @Autowired
    private PlatformRepository platformRepo;

    @BeforeEach
    void setup() {
        platformRepo.deleteAll();
    }

    @Test
    void shouldCreateAndRetrievePlatform() {
        Platform platform = Platform.builder()
                .name("Indeed").url("https://indeed.com")
                .urlTemplate("https://indeed.com/jobs?q={{QUERY}}")
                .type("jobboard").parserType(ParserType.JSOUP).build();

        ResponseEntity<ApiResponse<Platform>> res = platformService.createPlatform(platform, "admin", true);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(platformRepo.findAll()).hasSize(1);

        ResponseEntity<ApiResponse<Map<String, List<Platform>>>> platformsRes = platformService.getAllPlatforms("admin", true);
        assertThat(platformsRes.getBody()).isNotNull();
        assertThat(platformsRes.getBody().getData().get("all")).hasSize(1);
    }

    @Test
    void shouldUpdatePlatform() {
        Platform saved = platformRepo.save(Platform.builder()
                .publicId(UUID.randomUUID()).name("LinkedIn").url("https://linkedin.com")
                .type("jobboard").parserType(ParserType.SELENIUM).createdByUserId("admin").build());

        Platform updates = Platform.builder().name("UpdatedLinkedIn").build();
        ResponseEntity<ApiResponse<Platform>> res = platformService.updatePlatform(
                saved.getPublicId().toString(), updates, "admin", true);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        Platform updated = platformRepo.findById(saved.getPlatformId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("UpdatedLinkedIn");
    }

    @Test
    void shouldDeletePlatform() {
        Platform saved = platformRepo.save(Platform.builder()
                .publicId(UUID.randomUUID()).name("ToDelete").url("https://todelete.com")
                .type("jobboard").parserType(ParserType.JSOUP).createdByUserId("admin").build());

        ResponseEntity<ApiResponse<String>> res = platformService.deletePlatform(
                saved.getPublicId().toString(), "admin", true);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(platformRepo.findAll()).isEmpty();
    }
}