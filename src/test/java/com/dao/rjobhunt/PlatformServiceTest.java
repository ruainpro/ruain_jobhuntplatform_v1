package com.dao.rjobhunt;

import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.ParserType;
import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.repository.PlatformRepository;
import com.dao.rjobhunt.Service.PlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlatformServiceTest {

    @InjectMocks
    private PlatformService platformService;

    @Mock
    private PlatformRepository platformRepo;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPlatform_shouldSucceed_whenPlatformIsUnique() {
        Platform platform = Platform.builder()
                .name("Indeed")
                .url("https://www.indeed.com")
                .urlTemplate("https://www.indeed.com/jobs?q={{QUERY}}&l={{LOCATION}}")
                .type("jobboard")
                .parserType(ParserType.JSOUP)
                .build();

        when(platformRepo.existsByTypeIgnoreCase("jobboard")).thenReturn(false);
        when(platformRepo.existsByNameIgnoreCase("Indeed")).thenReturn(false);
        when(platformRepo.existsByUrl("https://www.indeed.com")).thenReturn(false);

        ResponseEntity<ApiResponse<Platform>> response = platformService.createPlatform(platform, "user-123", true);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Platform created successfully");

        verify(platformRepo, times(1)).save(any(Platform.class));
    }

    @Test
    void createPlatform_shouldFail_whenNameOrUrlExists() {
        Platform platform = Platform.builder()
                .name("Indeed")
                .url("https://www.indeed.com")
                .type("jobboard")
                .build();

        when(platformRepo.existsByNameIgnoreCase("Indeed")).thenReturn(true);

        ResponseEntity<ApiResponse<Platform>> response = platformService.createPlatform(platform, "user-123", true);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Platform with same name or URL already exists");

        verify(platformRepo, never()).save(any());
    }

    @Test
    void updatePlatform_shouldFail_whenUserIsNotOwner() {
        UUID publicId = UUID.randomUUID();
        Platform existing = Platform.builder()
                .publicId(publicId)
                .name("LinkedIn")
                .createdByUserId("another-user")
                .build();

        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        Platform updates = Platform.builder().name("UpdatedName").build();

        ResponseEntity<ApiResponse<Platform>> response = platformService.updatePlatform(
                publicId.toString(), updates, "current-user", false);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Not authorized");
    }

    @Test
    void deletePlatform_shouldSucceed_whenAdminDeletes() {
        UUID publicId = UUID.randomUUID();
        Platform existing = Platform.builder()
                .publicId(publicId)
                .name("LinkedIn")
                .build();

        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        ResponseEntity<ApiResponse<String>> response = platformService.deletePlatform(
                publicId.toString(), "admin-user", true);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("Platform deleted successfully");

        verify(platformRepo, times(1)).delete(existing);
    }

    @Test
    void deletePlatform_shouldReturn404_whenPlatformNotFound() {
        UUID publicId = UUID.randomUUID();

        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = platformService.deletePlatform(
                publicId.toString(), "user-123", true);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void getAllPlatforms_shouldReturnAll_whenAdmin() {
        List<Platform> platforms = List.of(
                Platform.builder().name("Indeed").type("jobboard").build(),
                Platform.builder().name("CustomBoard").type("custom").createdByUserId("user-123").build()
        );

        when(platformRepo.findAll()).thenReturn(platforms);

        ResponseEntity<ApiResponse<Map<String, List<Platform>>>> response =
                platformService.getAllPlatforms("admin", true);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).containsKey("all");
        assertThat(response.getBody().getData().get("all")).hasSize(2);
    }
}