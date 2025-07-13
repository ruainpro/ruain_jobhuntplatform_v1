package com.dao.rjobhunt.models;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean discordEnabled;

    private String phoneNumber;     // Optional: different from user profile phone
    private String discordWebhook;  // Optional: for Discord notifications
}