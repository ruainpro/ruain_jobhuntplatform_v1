package com.dao.rjobhunt;

import com.dao.rjobhunt.Service.SMSService;
import com.dao.rjobhunt.models.Job;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public class SMSServiceSimpleTest {

	@Autowired
	private  SMSService smsService;
	
	
    @Test
    public void testSendJobAlertWithValidJobList() {

//         Inject test credentials (should be dummy in test env)

        Job job = Job.builder()
                .title("Java Developer")
                .company("TestCompany")
                .location("Toronto")
                .url("https://example.com/job/123")
                .build();

        // This will try sending SMS if your Twilio setup is live
        smsService.sendJobAlert("+16474491794", List.of(job));
    }
}