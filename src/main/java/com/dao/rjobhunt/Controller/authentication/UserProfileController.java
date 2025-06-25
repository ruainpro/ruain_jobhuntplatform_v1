package com.dao.rjobhunt.Controller.authentication;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

	@Autowired
	private UserServices userServices;

	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UserInfoRepository infoRepository;

	@Operation(summary = "Get user by publicId", description = "Returns user details by public UUID. This avoids exposing internal _id.")
	@GetMapping()
	public ResponseEntity<ApiResponse<User>> getUserByPublicId() {

		String publicId = jwtService.getPublicIdFromCurrentRequest();
		return userServices.getUserByPublicId(UUID.fromString(publicId))
				.map(user -> ResponseEntity.ok(ApiResponse.success("User found", user))).orElseGet(() -> ResponseEntity
						.status(404).body(ApiResponse.error("User not found with publicId: " + publicId)));
	}

	@Operation(summary = "Update logged-in user's profile", description = "Allows an authenticated user to update their own profile (email, phone, gender, dob, address)")
	@PatchMapping()
	public ResponseEntity<ApiResponse<User>> updateOwnProfile(@RequestBody UserDto userDto) {
		try {
			// Extract publicId from JWT
			String publicId = jwtService.getPublicIdFromCurrentRequest();
			if (publicId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ApiResponse.error("Invalid or missing JWT token"));
			}

			// Update user via service
			User updatedUser = userServices.updateUserByPublicId(UUID.fromString(publicId), userDto);
			return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiResponse.error("Invalid input: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(ApiResponse.error("Something went wrong: " + e.getMessage()));
		}
	}

	@Operation(summary = "Get all users (Admin only)", description = "Returns a list of all users in the system. Requires ROLE_ADMIN")
	@GetMapping("/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
		List<User> users = userServices.getAllUsers(); // Should return List<UserDto>
		return ResponseEntity.ok(ApiResponse.success("All users retrieved successfully", users));
	}
	
	@GetMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<User>>> searchUsersByText(@RequestParam String query) {
	    if (query == null || query.trim().isEmpty()) {
	        return ResponseEntity.badRequest().body(ApiResponse.error("Query parameter is required"));
	    }

	    String cleanedQuery = query.trim();
	    Date parsedDate = null;
	    try {
	        parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(cleanedQuery);
	    } catch (ParseException ignored) {
	        // not a valid date, ignore
	    }

	    List<User> users = infoRepository.searchByTextAndDate(cleanedQuery, parsedDate);

	    return ResponseEntity.ok(ApiResponse.success(
	            users.isEmpty() ? "No users found matching: " + query : "Matching users found", users));
	}
	

	@Operation(summary = "Admin: Partially update any user's profile", description = "Allows admin to update email, phone number, gender, date of birth, or address of any user using their publicId")
	@PatchMapping("/admin/{publicId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<User>> patchUserByAdmin(@PathVariable String publicId,
			@RequestBody UserDto userDto) {

		try {
			Optional<User> optionalUser = userServices.getUserByPublicId(UUID.fromString(publicId));
			if (optionalUser.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ApiResponse.error("User with publicId " + publicId + " not found"));
			}

			User updatedUser = userServices.updateUserByPublicId(UUID.fromString(publicId), userDto);
			return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(ApiResponse.error("Failed to update user: " + e.getMessage()));
		}
	}

}
