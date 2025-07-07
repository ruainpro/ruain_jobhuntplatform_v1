package com.dao.rjobhunt.models;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @JsonIgnore
    @Id
    private String userId;

    private UUID publicId;
    private String email;
    
    @JsonIgnore
    private String password;
    private String role;

    private AccountStatus accountStatus; // Embedded document


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String phoneNumber;
    private String gender;
    private Date dateOfBirth;
    private String address;
    
    private List<String> preferredJobTitles;

}
