package com.dao.rjobhunt.dto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.Notification;
import com.dao.rjobhunt.models.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO for exposing user data")
public class UserDto {

	@Schema(description = "Public UUID of the user")
	private UUID publicId;

	@NotBlank
	@Email
	@Schema(description = "User email address", example = "john2.doe@example.com")
	private String email;

	@NotBlank
	@Size(min = 6)
	@Schema(description = "Password", example = "secret123")
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

	@Schema(description = "User phone number", example = "+1234567890")
	@NotBlank(message = "Phone number cannot be blank")
	@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in international format, e.g., +1234567890")
	private String phoneNumber;

	@Schema(description = "Gender of the user", example = "Male/Female/Other")
	private String gender;

	@Past
	@Schema(description = "Date of birth", example = "1995-08-17")
	private Date dateOfBirth;

	@Schema(description = "Postal address")
	private String address;

	@Schema(description = "User's role", example = "ROLE_USER")
	private String role;

	@Schema(description = "Account creation time")
	private LocalDateTime createdAt;

	@Schema(description = "Last account update time")
	private LocalDateTime updatedAt;

	@Schema(description = "List of preferred job titles")
	private List<String> preferredJobTitles;
	
	@Schema(description = "User notification preference config")
	private Notification notification;

	@Schema(description = "Account status info")
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private AccountStatusDto accountStatus;

	public UserDto(UUID publicId, String email, String password, String phoneNumber, String gender, Date dateOfBirth,
			String address, String role, LocalDateTime createdAt, LocalDateTime updatedAt,
			AccountStatusDto accountStatus, List<String> preferredJobTitles) {
		this.publicId = publicId;
		this.email = email;
		this.password = password;
		this.phoneNumber = phoneNumber;
		this.gender = gender;
		this.dateOfBirth = dateOfBirth;
		this.address = address;
		this.role = role;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.accountStatus = accountStatus;
		this.preferredJobTitles = preferredJobTitles;
	}
	
	public UserDto(UUID publicId, String email, String password, String phoneNumber, String gender, Date dateOfBirth,
			String address, String role, LocalDateTime createdAt, LocalDateTime updatedAt,
			AccountStatusDto accountStatus, List<String> preferredJobTitles,Notification notification) {
		this.publicId = publicId;
		this.email = email;
		this.password = password;
		this.phoneNumber = phoneNumber;
		this.gender = gender;
		this.dateOfBirth = dateOfBirth;
		this.address = address;
		this.role = role;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.accountStatus = accountStatus;
		this.preferredJobTitles = preferredJobTitles;
		this.notification = notification;

	}

	public static UserDto fromEntity(User user, AccountStatus status) {
	    UserDto dto = new UserDto(user.getPublicId(), user.getEmail(), user.getPassword(), user.getPhoneNumber(),
	        user.getGender(), user.getDateOfBirth(), user.getAddress(), user.getRole(), user.getCreatedAt(),
	        user.getUpdatedAt(), AccountStatusDto.fromEntity(status), user.getPreferredJobTitles());

	    dto.setNotification(user.getNotification()); // ✅ Set notification config
	    return dto;
	}

	public User toEntity() {
	    return User.builder()
	        .publicId(this.publicId != null ? this.publicId : UUID.randomUUID())
	        .email(this.email)
	        .phoneNumber(this.phoneNumber)
	        .gender(this.gender)
	        .dateOfBirth(this.dateOfBirth)
	        .address(this.address)
	        .role(this.role)
	        .createdAt(this.createdAt != null ? this.createdAt : LocalDateTime.now())
	        .updatedAt(LocalDateTime.now())
	        .preferredJobTitles(this.preferredJobTitles)
	        .notification(this.notification) // ✅ Include notification config
	        .build();
	}
}