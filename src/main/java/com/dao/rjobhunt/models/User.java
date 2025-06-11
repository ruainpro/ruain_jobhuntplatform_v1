package com.dao.rjobhunt.models;

import lombok.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Document(collection = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String userId;

    private UUID publicId;
    private String email;
    private String password;
    private String role;
    
    private AccountStatus accountStatus; // ✅ Embedded document

    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private String phoneNumber;
    private String gender;
    private Date dateOfBirth;
    private String address;
}
