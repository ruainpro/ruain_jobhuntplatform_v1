package com.dao.rjobhunt;

import com.dao.rjobhunt.Controller.authentication.AdminReportController;
import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.*;
import com.dao.rjobhunt.dto.*;
import com.dao.rjobhunt.models.*;
import com.dao.rjobhunt.repository.*;
import com.dao.rjobhunt.others.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mongodb.client.result.DeleteResult;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

class UnsafeAllocator {
    static Object create(Class<?> clazz) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            return unsafe.allocateInstance(clazz);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate class: " + clazz.getName(), e);
        }
    }
}

public class AllServiceTests {

	
	// ✅ Repositories (interfaces) → Only @Mock
	@Mock private ActionHistoryRepository actionHistoryRepository;
	@Mock private PlatformRepository platformRepo;
	@Mock private ScraperRequestRepository scraperRequestRepo;
	@Mock private UserInfoRepository userInfoRepository;

	// ✅ External Dependencies → Only @Mock
	@Mock private RestTemplate restTemplate;
	@Mock private JwtService jwtService;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private SpringTemplateEngine templateEngine;
	@Mock private RequestUtil requestUtil;
	@Mock private MongoTemplate mongoTemplate;

	// ✅ Services Under Test → @InjectMocks
	@InjectMocks private ActionHistoryServices actionHistoryServices;
	@InjectMocks private JobService jobService;
	@Mock private EmailService emailService;
	@InjectMocks private PlatformService platformService;
	@InjectMocks private ScraperService scraperService;
	@InjectMocks private IndeedScraperService indeedScraperService;
	@InjectMocks private UserServices userServices;
	@InjectMocks private DiscordService discordService;
	@InjectMocks private DiscordService injectedDiscordService;

	// ✅ Real job service with manually injected restTemplate (for reflection)
	private JobService realJobService;
	
	@Mock
    private ReportService reportService;
    
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject dependencies manually for real services
        realJobService = (JobService) UnsafeAllocator.create(JobService.class);
        Field rt = JobService.class.getDeclaredField("restTemplate");
        rt.setAccessible(true);
        rt.set(realJobService, restTemplate);

        Field discordRT = DiscordService.class.getDeclaredField("restTemplate");
        discordRT.setAccessible(true);
        discordRT.set(discordService, restTemplate);

        // 💥 Fix mockMvc (you missed this part earlier)
        AdminReportController reportController = new AdminReportController(reportService);
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
    }

	// ---------- ActionHistoryServices ----------
	@Test
	public void testAddActionHistory_shouldSetCorrectActionType() {
		String userId = "testUserId";
		String description = "Testing POST method";

		try (MockedStatic<RequestContext> mocked = mockStatic(RequestContext.class)) {
			mocked.when(RequestContext::getHttpMethod).thenReturn("POST");

			when(actionHistoryRepository.save(any(ActionHistory.class)))
					.thenAnswer(invocation -> invocation.getArgument(0));

			boolean result = actionHistoryServices.addActionHistory(userId, description);

			assertTrue(result);
			verify(actionHistoryRepository).save(argThat(action -> "CREATE".equals(action.getActionType())));
		}
	}

	// ---------- JobService ----------
	@Test
	void searchAdzunaJobs_shouldReturnResults() throws Exception {
	    // 1️⃣ Create a no-arg instance of JobService

	    // 2️⃣ Inject the mocked RestTemplate via reflection
	    Field rt = JobService.class.getDeclaredField("restTemplate");
	    realJobService = (JobService) UnsafeAllocator.create(JobService.class); // ✅ Bypass constructor
	    
	    rt.setAccessible(true);
	    rt.set(realJobService, restTemplate);

	    // 3️⃣ Inject other config fields via reflection
	    Field baseUrl  = JobService.class.getDeclaredField("baseUrl");
	    Field appId    = JobService.class.getDeclaredField("appId");
	    Field appKey   = JobService.class.getDeclaredField("appKey");
	    Field country  = JobService.class.getDeclaredField("country");

	    baseUrl.setAccessible(true); baseUrl.set(realJobService, "https://api.adzuna.com/v1/api/jobs");
	    appId.setAccessible(true);   appId.set(realJobService, "16920d25");
	    appKey.setAccessible(true);  appKey.set(realJobService, "82613a24ad5ccf0c2232b97d54dc6d1d");
	    country.setAccessible(true); country.set(realJobService, "ca");

	    // 4️⃣ Mock external call
	    JsonNode mockResponse = JsonNodeFactory.instance.objectNode().put("count", 16735);
	    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
	            .thenReturn(ResponseEntity.ok(mockResponse));

	    // 5️⃣ Execute and verify
	    JsonNode response = realJobService.searchAdzunaJobs("developer", "Toronto", null, "date", 1);

	    assertThat(response).isNotNull();
	    assertThat(response.get("count").asInt()).isEqualTo(16735);
	}

	// ---------- EmailService (Commented out to prevent real email sending)
	// ----------
	// @Test
	// public void testSendJobAlertEmail() {
	// Map<String, Object> vars = Map.of(
	// "USER_NAME", "Raj",
	// "JOB_TITLE", "Full Stack Developer",
	// "COMPANY_NAME", "Tech Inc",
	// "LOCATION", "Toronto",
	// "SALARY", "$100k",
	// "DESCRIPTION", "Job Description...",
	// "JOB_URL", "https://example.com",
	// "SENT_DATE", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d,
	// yyyy"))
	// );

	// try {
	// emailService.sendJobAlertEmail("test@example.com", vars);
	// System.out.println("✅ Email sent.");
	// } catch (Exception e) {
	// e.printStackTrace();
	// fail("❌ Failed to send email.");
	// }
	// }

	// ---------- PlatformService ----------
	@Test
	void shouldCreateAndRetrievePlatform() {
		Platform platform = Platform.builder().name("Indeed").url("https://indeed.com")
				.urlTemplate("https://indeed.com/jobs?q={{QUERY}}").type("jobboard").parserType(ParserType.JSOUP)
				.build();

		when(platformRepo.save(any())).thenReturn(platform);
		when(platformRepo.findAll()).thenReturn(List.of(platform));

		ApiResponse<Platform> response = platformService.createPlatform(platform, "admin", true).getBody();
		assertThat(response).isNotNull();
		assertThat(response.isSuccess()).isTrue();
	}

	@Test
	void shouldUpdatePlatform() {
		Platform saved = Platform.builder().publicId(UUID.randomUUID()).name("LinkedIn").url("https://linkedin.com")
				.type("jobboard").parserType(ParserType.SELENIUM).createdByUserId("admin").build();

		when(platformRepo.findByPublicId(saved.getPublicId())).thenReturn(Optional.of(saved));

		Platform updates = Platform.builder().name("UpdatedLinkedIn").build();
		ResponseEntity<ApiResponse<Platform>> res = platformService.updatePlatform(saved.getPublicId().toString(),
				updates, "admin", true);

		assertThat(res.getBody()).isNotNull();
		assertThat(res.getBody().isSuccess()).isTrue();
	}

	@Test
	void shouldDeletePlatform() {
		Platform saved = Platform.builder().publicId(UUID.randomUUID()).name("ToDelete").url("https://todelete.com")
				.type("jobboard").parserType(ParserType.JSOUP).createdByUserId("admin").build();

		when(platformRepo.findByPublicId(saved.getPublicId())).thenReturn(Optional.of(saved));

		ResponseEntity<ApiResponse<String>> res = platformService.deletePlatform(saved.getPublicId().toString(),
				"admin", true);

		assertThat(res.getBody()).isNotNull();
		assertThat(res.getBody().isSuccess()).isTrue();
	}

// ---------- DiscordService ----------
	@Test
	public void testSendJobAlert_sendsToDiscordWebhook() {
		String webhookUrl = "https://discord.com/api/webhooks/mock";

		Job job = Job.builder().title("Java Developer").company("RuAin Labs").location("Toronto").salary("$100k")
				.url("https://job.url/test").build();

		when(restTemplate.postForEntity(eq(webhookUrl), any(), eq(String.class)))
				.thenReturn(new ResponseEntity<>("OK", HttpStatus.NO_CONTENT));

		discordService.sendJobAlert(webhookUrl, List.of(job));
		verify(restTemplate, times(1)).postForEntity(eq(webhookUrl), any(), eq(String.class));
	}

// ---------- UserService ----------
	@Test
	void testGetUserByPublicId() {
		UUID testPublicId = UUID.randomUUID();
		User testUser = new User();
		testUser.setPublicId(testPublicId);
		testUser.setEmail("test@example.com");

		when(userInfoRepository.findByPublicId(testPublicId)).thenReturn(Optional.of(testUser));

		Optional<User> result = userServices.getUserByPublicId(testPublicId);

		assertTrue(result.isPresent());
		assertEquals("test@example.com", result.get().getEmail());
	}

	@Test
	void testGetAllUsers() {

		/* 🛠️ 3. Create a User instance with builder (or no-arg ctor + setters) */
		List<User> userList = List.of(User.builder().email("test@example.com").build());

		when(userInfoRepository.findAll()).thenReturn(userList);

		List<User> result = userServices.getAllUsers();

		assertEquals(1, result.size());
		assertEquals("test@example.com", result.get(0).getEmail());
	}

	@Test
	void testUpdateUserByPublicId() {
		UUID testPublicId = UUID.randomUUID();
		User testUser = new User();
		testUser.setPublicId(testPublicId);
		testUser.setEmail("old@example.com");

		UserDto dto = new UserDto();
		dto.setEmail("new@example.com");
		dto.setAddress("New Street");

		when(userInfoRepository.findByPublicId(testPublicId)).thenReturn(Optional.of(testUser));
		when(userInfoRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

		User result = userServices.updateUserByPublicId(testPublicId, dto);

		assertEquals("new@example.com", result.getEmail());
		assertEquals("New Street", result.getAddress());
	}

	@Test
	void testSearchByTextAndDate() throws Exception {
		String query = "test";
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2024-01-01");
		User testUser = new User();
		testUser.setEmail("test@example.com");

		when(userInfoRepository.searchByTextAndDate(query, date)).thenReturn(List.of(testUser));

		List<User> result = userInfoRepository.searchByTextAndDate(query, date);

		assertEquals(1, result.size());
		assertEquals("test@example.com", result.get(0).getEmail());
	}

	@Test
	void testGenerateAndSendNewPassword_Success() {
		User user = new User();
		user.setEmail("test@example.com");
		user.setPassword("oldpass");
		user.setAccountStatus(AccountStatus.builder().token("abc123").build());

		when(userInfoRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(requestUtil.generateRandomPassword(10)).thenReturn("newpass123");
		when(passwordEncoder.encode("newpass123")).thenReturn("encodedpass");
		when(templateEngine.process(eq("email/forgot_password"), any(Context.class))).thenReturn("<html>Reset</html>");
		when(userInfoRepository.save(any(User.class))).thenReturn(user);

		User updatedUser = userServices.generateAndSendNewPassword("test@example.com");

		assertEquals("encodedpass", updatedUser.getPassword());
		assertNotNull(updatedUser.getUpdatedAt());
	}

	@Test
	void testRegisterUser_EmailExists_ThrowsException() {
		User user = new User();
		user.setEmail("test@example.com");
		when(userInfoRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

		UserDto dto = new UserDto();
		dto.setEmail("test@example.com");

		assertThrows(IllegalArgumentException.class, () -> userServices.registerUser(dto));
	}

	@Test
	void testVerifyAccountByToken_AlreadyVerified() {
		User user = new User();
		user.setAccountStatus(AccountStatus.builder().statusId(1).token("abc123").build());

		when(userInfoRepository.findByAccountStatus_Token("abc123")).thenReturn(Optional.of(user));

		assertThrows(IllegalArgumentException.class, () -> userServices.verifyAccountByToken("abc123"));
	}

	@Test
	void testVerifyAccountByToken_ExpiredToken() {
		User user = new User();
		user.setAccountStatus(AccountStatus.builder().statusId(0).token("abc123")
				.createdAt(LocalDateTime.now().minusHours(25)).expiresAt(LocalDateTime.now().minusHours(1)).build());

		when(userInfoRepository.findByAccountStatus_Token("abc123")).thenReturn(Optional.of(user));

		assertThrows(IllegalArgumentException.class, () -> userServices.verifyAccountByToken("abc123"));
	}

	@Test
	void testGenerateAndSendNewPassword_UserNotFound() {
		when(userInfoRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class,
				() -> userServices.generateAndSendNewPassword("missing@example.com"));
	}
	
    @Test
    void testGetTotalUsers() throws Exception {
        when(reportService.getTotalRegisteredUsers()).thenReturn(100L);

        mockMvc.perform(get("/api/admin/reports/users/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void testGetActiveVsInactiveUsers() throws Exception {
        when(reportService.getActiveInactiveUsers()).thenReturn(Map.of("Active", 80L, "Inactive", 20L));

        mockMvc.perform(get("/api/admin/reports/users/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Active").value(80))
                .andExpect(jsonPath("$.Inactive").value(20));
    }

    @Test
    void testGetUserGrowth() throws Exception {
        List<UserGrowthDto> growth = List.of(new UserGrowthDto("2025-07-01", 10));
        when(reportService.aggregateUserGrowth(Mockito.anyString(), Mockito.anyString())).thenReturn(growth);

        mockMvc.perform(get("/api/admin/reports/users/growth")
                        .param("startDate", "2025-06-01")
                        .param("endDate", "2025-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2025-07-01"))
                .andExpect(jsonPath("$[0].count").value(10));
    }

    @Test
    void testGetTopScrapeQueries() throws Exception {
        when(reportService.getTopScrapeQueries()).thenReturn(Map.of("python jobs", 25L));

        mockMvc.perform(get("/api/admin/reports/scrapers/top-queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['python jobs']").value(25));
    }

    @Test
    void testGetNotificationPreferencesUsage() throws Exception {
        when(reportService.getNotificationPreferencesUsage()).thenReturn(Map.of("Email", 50L, "SMS", 30L, "Discord", 20L));

        mockMvc.perform(get("/api/admin/reports/users/notification-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Email").value(50))
                .andExpect(jsonPath("$.SMS").value(30))
                .andExpect(jsonPath("$.Discord").value(20));
    }

    @Test
    void testGetLoginActivity() throws Exception {
        TimeSeriesDto dto = TimeSeriesDto.builder()
                .date(LocalDate.parse("2025-07-01"))
                .count(5L)
                .build();

        when(reportService.getLoginActivityOverTime()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reports/activity/logins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2025-07-01"))
                .andExpect(jsonPath("$[0].count").value(5));
    }

    @Test
    void testGetTotalJobsNotified() throws Exception {
        when(reportService.getTotalJobsNotified()).thenReturn(77L);

        mockMvc.perform(get("/api/admin/reports/notifications/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("77"));
    }

    @Test
    void testGetAutoVsManualNotificationRatio() throws Exception {
        when(reportService.getAutoVsManualNotificationRatio()).thenReturn(Map.of("Auto", 40L, "Manual", 60L));

        mockMvc.perform(get("/api/admin/reports/notifications/type-ratio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Auto").value(40))
                .andExpect(jsonPath("$.Manual").value(60));
    }

}