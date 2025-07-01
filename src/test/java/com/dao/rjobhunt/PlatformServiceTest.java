package com.dao.rjobhunt;

import com.dao.rjobhunt.Service.PlatformService;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.ParserType;
import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.repository.PlatformRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

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
    void createPlatform_shouldSucceed_whenUnique() {
        Platform platform = Platform.builder()
                .name("Indeed").url("https://indeed.com")
                .type("jobboard").parserType(ParserType.JSOUP).build();

        when(platformRepo.existsByTypeIgnoreCase("jobboard")).thenReturn(false);
        when(platformRepo.existsByNameIgnoreCase("Indeed")).thenReturn(false);
        when(platformRepo.existsByUrl("https://indeed.com")).thenReturn(false);

        ResponseEntity<ApiResponse<Platform>> res = platformService.createPlatform(platform, "user-123", true);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        verify(platformRepo, times(1)).save(any(Platform.class));
    }

    @Test
    void createPlatform_shouldFail_whenNameExists() {
        Platform platform = Platform.builder().name("Indeed").url("https://indeed.com").type("jobboard").build();
        when(platformRepo.existsByNameIgnoreCase("Indeed")).thenReturn(true);

        ResponseEntity<ApiResponse<Platform>> res = platformService.createPlatform(platform, "user-123", true);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        verify(platformRepo, never()).save(any());
    }

    @Test
    void updatePlatform_shouldSucceed_whenAdminOrOwner() {
        UUID publicId = UUID.randomUUID();
        Platform existing = Platform.builder().publicId(publicId).name("LinkedIn").createdByUserId("user-123").build();
        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        Platform updates = Platform.builder().name("UpdatedLinkedIn").build();

        ResponseEntity<ApiResponse<Platform>> res = platformService.updatePlatform(
                publicId.toString(), updates, "user-123", false);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(existing.getName()).isEqualTo("UpdatedLinkedIn");
        verify(platformRepo, times(1)).save(existing);
    }

    @Test
    void updatePlatform_shouldFail_whenUnauthorized() {
        UUID publicId = UUID.randomUUID();
        Platform existing = Platform.builder().publicId(publicId).name("LinkedIn").createdByUserId("another-user").build();
        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        Platform updates = Platform.builder().name("Updated").build();

        ResponseEntity<ApiResponse<Platform>> res = platformService.updatePlatform(
                publicId.toString(), updates, "user-123", false);

        assertThat(res.getStatusCodeValue()).isEqualTo(403);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isFalse();
        verify(platformRepo, never()).save(any());
    }

    @Test
    void deletePlatform_shouldSucceed_whenAdminOrOwner() {
        UUID publicId = UUID.randomUUID();
        Platform existing = Platform.builder().publicId(publicId).name("LinkedIn").createdByUserId("user-123").build();
        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        ResponseEntity<ApiResponse<String>> res = platformService.deletePlatform(
                publicId.toString(), "user-123", false);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        verify(platformRepo, times(1)).delete(existing);
    }

    @Test
    void deletePlatform_shouldFail_whenUnauthorized() {
        UUID publicId = UUID.randomUUID();
        Platform existing = Platform.builder().publicId(publicId).name("LinkedIn").createdByUserId("another-user").build();
        when(platformRepo.findByPublicId(publicId)).thenReturn(Optional.of(existing));

        ResponseEntity<ApiResponse<String>> res = platformService.deletePlatform(
                publicId.toString(), "user-123", false);

        assertThat(res.getStatusCodeValue()).isEqualTo(403);
        assertThat(res.getBody().isSuccess()).isFalse();
        verify(platformRepo, never()).delete(any());
    }

    @Test
    void getAllPlatforms_shouldReturnAdminView() {
        List<Platform> platforms = List.of(
                Platform.builder().name("Indeed").type("jobboard").build(),
                Platform.builder().name("Custom").type("custom").createdByUserId("user-123").build());
        when(platformRepo.findAll()).thenReturn(platforms);

        ResponseEntity<ApiResponse<Map<String, List<Platform>>>> res = platformService.getAllPlatforms("admin", true);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(res.getBody().getData().get("all")).hasSize(2);
    }

    @Test
    void getAllPlatforms_shouldReturnUserView() {
        List<Platform> platforms = List.of(
                Platform.builder().name("Indeed").type("jobboard").build(),
                Platform.builder().name("Custom").type("custom").createdByUserId("user-123").build(),
                Platform.builder().name("OtherCustom").type("custom").createdByUserId("someone-else").build());
        when(platformRepo.findAll()).thenReturn(platforms);

        ResponseEntity<ApiResponse<Map<String, List<Platform>>>> res = platformService.getAllPlatforms("user-123", false);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isSuccess()).isTrue();
        assertThat(res.getBody().getData().get("common")).hasSize(1); // Indeed
        assertThat(res.getBody().getData().get("custom")).hasSize(1); // Custom by user-123
    }
}
