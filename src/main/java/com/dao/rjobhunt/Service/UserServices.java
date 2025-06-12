package com.dao.rjobhunt.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.others.RequestUtil;
import com.dao.rjobhunt.repository.UserInfoRepository;

import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotNull;

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

	private final RequestUtil requestUtil = new RequestUtil();

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
				.expiresAt(LocalDateTime.now().plusHours(24)).build());

		// Save to MongoDB
		User savedUser = userInfoRepository.save(user);

		if (Objects.nonNull(savedUser)) {
			String verificationLink = requestUtil.getBaseUrl() + "/auth/verify?token="
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
	    boolean returnStatus= userInfoRepository.save(user) != null;

	    return returnStatus;
	}


}
