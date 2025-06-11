package com.dao.rjobhunt.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
public class UserServices {
	
	@Autowired
	private UserInfoRepository userInfoRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	
    public UserDto registerUser(User userMd) {
        // Check for duplicate email
        Optional<User> existing = userInfoRepository.findByEmail(userMd.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Populate user fields
        userMd.setPublicId(UUID.randomUUID());
        userMd.setPassword(passwordEncoder.encode(userMd.getPassword()));
        userMd.setRole("ROLE_USER");
        userMd.setCreatedAt(LocalDateTime.now());
        userMd.setUpdatedAt(LocalDateTime.now());

        // Attach account status with token
        userMd.setAccountStatus(
        	    AccountStatus.builder()
        	        .accountStatusId(1)
        	        .statusId(1)
        	        .token(UUID.randomUUID().toString())
        	        .createdAt(LocalDateTime.now())
        	        .expiresAt(LocalDateTime.now().plusHours(24))
        	        .build()
        	);

        // Save to MongoDB
        userInfoRepository.save(userMd);

        // Return DTO
        return UserDto.fromEntity(userMd, userMd.getAccountStatus());
    }

}
