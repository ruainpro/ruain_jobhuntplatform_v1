package com.dao.rjobhunt.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.others.RequestUtil;
import com.dao.rjobhunt.repository.UserInfoRepository;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;

@Service
public class UserServices {

	@Autowired
	private UserInfoRepository userInfoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmailService emailService;

	@Autowired
	private SpringTemplateEngine templateEngine;

	@Value("${frontend.base.url}")
	private String frontendBaseUrl;
	
	@Autowired
	private RequestUtil requestUtil;

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
		user.setPassword(passwordEncoder.encode(userDto.getPassword())); // You must add 'password' to UserDto or pass
																			// it separately

		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());

		// Attach account status with token
		user.setAccountStatus(AccountStatus.builder().accountStatusId(1).statusId(0) // 0 = inactive
				.token(UUID.randomUUID().toString()).createdAt(LocalDateTime.now())
				.tokenType("verification")
				.expiresAt(LocalDateTime.now().plusHours(24)).build());

		// Save to MongoDB
		User savedUser = userInfoRepository.save(user);

		if (Objects.nonNull(savedUser)) {
			String verificationLink = frontendBaseUrl + "/verify?token="
					+ savedUser.getAccountStatus().getToken();

			Context context = new Context();
			context.setVariable("USER_NAME", savedUser.getEmail());
			context.setVariable("USER_EMAIL", savedUser.getEmail());
			context.setVariable("CREATED_DATE", savedUser.getCreatedAt().toString());
			context.setVariable("VERIFICATION_LINK", verificationLink);

			try {
				String htmlContent = templateEngine.process("email/verify-email", context); // No ".html" required
				emailService.sendHtmlEmail(savedUser.getEmail(), "Verify your email - RuAin", htmlContent);
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		// Return as DTO
		return UserDto.fromEntity(savedUser, savedUser.getAccountStatus());
	}
	
	public boolean verifyAccountByToken(String token) {
	    User user = userInfoRepository.findByAccountStatus_Token(token)
	            .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

	    if (user.getAccountStatus().getStatusId() == 1) {
	        throw new IllegalArgumentException("User already verified.");
	    }

	    LocalDateTime expiry = user.getAccountStatus().getCreatedAt().plusHours(24);
	    if (LocalDateTime.now().isAfter(expiry)) {
	        throw new IllegalArgumentException("Verification token expired.");
	    }

	    user.getAccountStatus().setStatusId(1); // Mark as verified
	    user.getAccountStatus().setToken(null);
	    user.getAccountStatus().setTokenType(null);
	    boolean returnStatus= userInfoRepository.save(user) != null;

	    return returnStatus;
	}
	
	
	public User generateAndSendNewPassword(String email) {
	    User user = userInfoRepository.findByEmail(email)
	            .orElseThrow(() -> new IllegalArgumentException("Email not found"));

	    // Generate random password
	    String newPassword = requestUtil.generateRandomPassword(10);  // Make sure this method is defined

	    // Save encoded password
	    user.setPassword(passwordEncoder.encode(newPassword));  // passwordEncoder must be autowired
	    user.setUpdatedAt(LocalDateTime.now());               // optional: track change
	    userInfoRepository.save(user);

	    // Prepare Thymeleaf email context
	    Context context = new Context();
	    context.setVariable("USER_EMAIL", user.getEmail());
	    context.setVariable("NEW_PASSWORD", newPassword);
	    context.setVariable("CREATED_DATE", LocalDateTime.now().toString());

	    try {
	        String htmlContent = templateEngine.process("email/forgot_password", context); // must exist in `resources/templates/email`
	        emailService.sendHtmlEmail(user.getEmail(), "Your New Password - RuAin", htmlContent);
	    } catch (Exception e) {
	        throw new RuntimeException("Error sending new password email", e);
	    }
	    
	    return user;
	}
	
	public List<User> getAllUsers() {
	    return userInfoRepository.findAll();
	}

	// Way to get the user details by public id
	public Optional<User> getUserByPublicId(UUID id) {
		
		return userInfoRepository.findByPublicId(id);
	}
	
	public User updateUserByPublicId(UUID publicId, UserDto userDto) {
	    User existingUser = userInfoRepository.findByPublicId(publicId)
	        .orElseThrow(() -> new IllegalArgumentException("User not found"));

	    // Update only non-null fields from DTO
	    if (userDto.getEmail() != null) {
	        existingUser.setEmail(userDto.getEmail());
	    }
	    if (userDto.getPhoneNumber() != null) {
	        existingUser.setPhoneNumber(userDto.getPhoneNumber());
	    }
	    if (userDto.getGender() != null) {
	        existingUser.setGender(userDto.getGender());
	    }
	    if (userDto.getDateOfBirth() != null) {
	        existingUser.setDateOfBirth(userDto.getDateOfBirth());
	    }
	    if (userDto.getAddress() != null) {
	        existingUser.setAddress(userDto.getAddress());
	    }

	    // Save updated user
	    User updatedUser = userInfoRepository.save(existingUser);

	    // Return sanitized DTO
	    return updatedUser;
	}


}
