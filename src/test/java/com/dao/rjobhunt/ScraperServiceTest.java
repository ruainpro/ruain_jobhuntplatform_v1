package com.dao.rjobhunt;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.*;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.*;
import com.dao.rjobhunt.repository.PlatformRepository;
import com.dao.rjobhunt.repository.ScraperRequestRepository;
import com.mongodb.client.result.DeleteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScraperServiceTest {

    @InjectMocks
    private ScraperService scraperService;

    @Mock
    private PlatformRepository platformRepo;

    @Mock
    private ScraperRequestRepository scraperRequestRepo;

    @Mock
    private IndeedScraperService indeedScraperService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ActionHistoryServices actionHistoryServices;

    @Mock
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void startScraping_shouldStartScrapingTask() throws Exception {
        ScraperRequest req = new ScraperRequest();
        req.setQuery("Java Dev");
        req.setLocation("Toronto");
        req.setPlatformId(UUID.randomUUID().toString());
        req.setMaxPages(1);

        String userId = UUID.randomUUID().toString();
        Platform platform = Platform.builder()
                .publicId(UUID.fromString(req.getPlatformId()))
                .name("Indeed")
                .parserType(ParserType.API)
                .apiKey("dummy-api-key")
                .build();

        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(userId);
        when(platformRepo.findByPublicId(UUID.fromString(req.getPlatformId()))).thenReturn(Optional.of(platform));
        when(indeedScraperService.scrapeIndeed(any(), any(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<String>> response = scraperService.startScraping(req);

        assertNotNull(response);
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Async scraping started"));
        verify(platformRepo).findByPublicId(any(UUID.class));
    }

    @Test
    void stopScraping_shouldStopActiveTask() {
        String userId = UUID.randomUUID().toString();
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(userId);

        ResponseEntity<ApiResponse<String>> response = scraperService.stopScraping();

        assertNotNull(response);
        assertTrue(response.getBody().isSuccess());
        assertEquals("Scraping stop requested", response.getBody().getMessage());
    }

    @Test
    void createScraperRequest_shouldSaveRequest() {
        ScraperRequest request = new ScraperRequest();
        when(scraperRequestRepo.save(any(ScraperRequest.class))).thenReturn(request);
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(UUID.randomUUID().toString());

        ResponseEntity<ApiResponse<ScraperRequest>> response = scraperService.createScraperRequest(request);

        assertTrue(response.getBody().isSuccess());
        verify(scraperRequestRepo).save(any(ScraperRequest.class));
        verify(actionHistoryServices).addActionHistory(any(), contains("[Scraper] Created ScraperRequest"));
    }

    @Test
    void getAllScraperRequests_shouldReturnList() {
        when(scraperRequestRepo.findAll()).thenReturn(List.of(new ScraperRequest()));

        ResponseEntity<ApiResponse<List<ScraperRequest>>> response = scraperService.getAllScraperRequests();

        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getScraperRequestsByCurrentUser_shouldReturnUserRequests() {
        UUID userUuid = UUID.randomUUID();
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(userUuid.toString());
        when(scraperRequestRepo.findByUserId(userUuid)).thenReturn(List.of(new ScraperRequest()));

        ResponseEntity<ApiResponse<List<ScraperRequest>>> response = scraperService.getScraperRequestsByCurrentUser();

        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        verify(actionHistoryServices).addActionHistory(any(), contains("[Scraper] Retrieved ScraperRequests"));
    }

    @Test
    void getScraperRequestByPublicId_shouldReturnRequestIfFound() {
        UUID publicId = UUID.randomUUID();
        ScraperRequest req = new ScraperRequest();
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(UUID.randomUUID().toString());
        when(scraperRequestRepo.findByPublicId(publicId)).thenReturn(Optional.of(req));

        ResponseEntity<ApiResponse<ScraperRequest>> response = scraperService.getScraperRequestByPublicId(publicId);

        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
        verify(actionHistoryServices).addActionHistory(any(), contains("[Scraper] Retrieved ScraperRequest"));
    }

    @Test
    void getScraperRequestByPublicId_shouldReturnErrorIfNotFound() {
        UUID publicId = UUID.randomUUID();
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(UUID.randomUUID().toString());
        when(scraperRequestRepo.findByPublicId(publicId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<ScraperRequest>> response = scraperService.getScraperRequestByPublicId(publicId);

        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("not found"));
    }

    @Test
    void deleteScraperRequestsByCurrentUser_shouldDeleteAndReturnCount() {
        String userId = UUID.randomUUID().toString();
        DeleteResult mockedDelete = DeleteResult.acknowledged(5);

        when(mongoTemplate.remove(any(Query.class), eq(ScraperRequest.class))).thenReturn(mockedDelete);

        ResponseEntity<ApiResponse<String>> response = scraperService.deleteScraperRequestsByCurrentUser(userId);

        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Deleted 5 scraper request"));
    }

    @Test
    void deleteScraperRequest_shouldDeleteIfExists() {
        UUID publicId = UUID.randomUUID();
        ScraperRequest req = new ScraperRequest();
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(UUID.randomUUID().toString());
        when(scraperRequestRepo.findByPublicId(publicId)).thenReturn(Optional.of(req));

        ResponseEntity<ApiResponse<String>> response = scraperService.deleteScraperRequest(publicId);

        assertTrue(response.getBody().isSuccess());
        verify(scraperRequestRepo).delete(req);
        verify(actionHistoryServices).addActionHistory(any(), contains("[Scraper] Deleted ScraperRequest"));
    }

    @Test
    void deleteScraperRequest_shouldReturnErrorIfNotFound() {
        UUID publicId = UUID.randomUUID();
        when(jwtService.getPublicIdFromCurrentRequest()).thenReturn(UUID.randomUUID().toString());
        when(scraperRequestRepo.findByPublicId(publicId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = scraperService.deleteScraperRequest(publicId);

        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("not found"));
    }
}
