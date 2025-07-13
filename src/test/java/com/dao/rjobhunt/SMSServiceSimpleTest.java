package com.dao.rjobhunt;

import com.dao.rjobhunt.Service.DiscordService;
import com.dao.rjobhunt.Service.SMSService;
import com.dao.rjobhunt.models.Job;

import com.dao.rjobhunt.Service.DiscordService;
import com.dao.rjobhunt.models.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.List;


public class SMSServiceSimpleTest {

	@Autowired
	private  SMSService smsService;
	
    @InjectMocks
    private DiscordService discordService;
    
    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }
	
	
//    @Test
//    public void testSendJobAlertWithValidJobList() {
//
////         Inject test credentials (should be dummy in test env)
//
//        Job job = Job.builder()
//                .title("Java Developer")
//                .company("TestCompany")
//                .location("Toronto")
//                .url("https://example.com/job/123")
//                .build();
//
//        // This will try sending SMS if your Twilio setup is live
//        smsService.sendJobAlert("+16474491794", List.of(job));
//    }
    


    @Test
    public void testSendJobAlert_sendsToDiscordWebhook() {
        // Arrange
        String webhookUrl = "https://discord.com/api/webhooks/1319508688406446111/Vzmn7QJinHUGh-qRVqzPG7DgN6fN9jqHb19QVDny04L96DF0Q0hLDbmLTsXXnMpioK8_";

        Job job = Job.builder()
                .title("Java Developer")
                .company("RuAin Labs")
                .location("Toronto")
                .salary("$100k")
                .url("https://job.url/test")
                .build();

        when(restTemplate.postForEntity(eq(webhookUrl), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.NO_CONTENT));

        // Act
        discordService.sendJobAlert(webhookUrl, List.of(job));

        // Assert
        verify(restTemplate, times(1)).postForEntity(eq(webhookUrl), any(), eq(String.class));
    }

}