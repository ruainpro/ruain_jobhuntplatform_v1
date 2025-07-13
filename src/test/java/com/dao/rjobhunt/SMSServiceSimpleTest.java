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
//        smsService.setAccountSid("AC02f3af1ac83d9e4784da7a83d483551a");
//        smsService.setAuthToken("405c3c0caa342b4612e3ef849ff5d835");
//        smsService.setFromNumber("+16474491794");  // Your Twilio number

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