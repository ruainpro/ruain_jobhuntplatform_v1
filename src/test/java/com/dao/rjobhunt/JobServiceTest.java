package com.dao.rjobhunt;

import com.dao.rjobhunt.Service.JobService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @InjectMocks
    private JobService jobService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        setField(jobService, "baseUrl", "https://api.adzuna.com/v1/api/jobs");
        setField(jobService, "country", "ca");
        setField(jobService, "appId", "16920d25");
        setField(jobService, "appKey", "82613a24ad5ccf0c2232b97d54dc6d1d");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void searchAdzunaJobs_shouldReturnResults() {
    	JsonNode mockResponse = JsonNodeFactory.instance.objectNode().put("count", 16735);
    	ResponseEntity<JsonNode> entity = ResponseEntity.ok(mockResponse);
    	Mockito.when(restTemplate.exchange(
    	        anyString(),
    	        eq(HttpMethod.GET),
    	        any(),
    	        eq(JsonNode.class)))
    	        .thenReturn(entity);

    	JsonNode response = jobService.searchAdzunaJobs("developer", "Toronto", null, "date", 1);

    	assertThat(response).isNotNull();
    	assertThat(response.get("count").asInt()).isEqualTo(16735); // match mocked value
    }


}