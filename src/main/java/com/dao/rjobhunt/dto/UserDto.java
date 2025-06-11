package com.dao.rjobhunt.dto;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

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
    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "User phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "Gender of the user", example = "Male/Female/Other")
    private String gender;

    @Past
    @Schema(description = "Date of birth", example = "1995-08-17")
    private LocalDate dateOfBirth;

    @Schema(description = "Postal address")
    private String address;

    @Schema(description = "User's role", example = "admin/user")
    private String role;

    @Schema(description = "Account creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last account update time")
    private LocalDateTime updatedAt;

    @Schema(description = "Account status info")
    private AccountStatusDto accountStatus;

    public static UserDto fromEntity(User user, AccountStatus status) {
        return new UserDto(
            user.getPublicId(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getGender(),
            user.getDateOfBirth(),
            user.getAddress(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            AccountStatusDto.fromEntity(status)
        );
    }

	public UserDto(UUID publicId2, String email2, String phoneNumber2, String gender2, Date dateOfBirth2,
			String address2, String role2, LocalDateTime createdAt2, LocalDateTime updatedAt2, AccountStatusDto fromEntity) {
		// TODO Auto-generated constructor stub
	}
}
