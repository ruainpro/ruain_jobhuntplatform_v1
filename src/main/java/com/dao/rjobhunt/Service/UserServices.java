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

@Service
public class UserServices {

	@Autowired
	private UserInfoRepository userInfoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;


	public UserDto registerUser(UserDto userDto) {
	    // Check for duplicate email
	    Optional<User> existing = userInfoRepository.findByEmail(userDto.getEmail());
	    if (existing.isPresent()) {
	        throw new IllegalArgumentException("Email already exists");
	    }

	    // Convert DTO to entity
	    User user = userDto.toEntity();

	    // Set additional fields
	    user.setPublicId(UUID.randomUUID());
	    user.setPassword(passwordEncoder.encode(userDto.getPassword())); // You must add 'password' to UserDto or pass it separately
	    user.setRole("ROLE_USER");
	    user.setCreatedAt(LocalDateTime.now());
	    user.setUpdatedAt(LocalDateTime.now());

	    // Attach account status with token
	    user.setAccountStatus(AccountStatus.builder()
	            .accountStatusId(1)
	            .statusId(0) // 0 = inactive
	            .token(UUID.randomUUID().toString())
	            .createdAt(LocalDateTime.now())
	            .expiresAt(LocalDateTime.now().plusHours(24))
	            .build()
	    );

	    // Save to MongoDB
	    User savedUser = userInfoRepository.save(user);

	    // Return as DTO
	    return UserDto.fromEntity(savedUser, savedUser.getAccountStatus());
	}


}
