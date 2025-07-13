package com.dao.rjobhunt.Controller.authentication;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.Notification;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

	@Autowired
	private UserServices userServices;

	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UserInfoRepository infoRepository;
	
	@Autowired
	private ActionHistoryServices actionHistoryServices;

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
			
			actionHistoryServices.addActionHistory(publicId,  ":User Updateds profile");
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
	
    @PatchMapping("/deactivateAccount")
    public ResponseEntity<?> updateStatusOfAccount(  ) {
        try {
			String publicId = jwtService.getPublicIdFromCurrentRequest();
			if (publicId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(ApiResponse.error("Invalid or missing JWT token"));
			}
            User updated = userServices.updateAccountStatusByPublicId(UUID.fromString(publicId), 2);
            return ResponseEntity.ok("Account status updated for user with publicId: " + updated.getPublicId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
			actionHistoryServices.addActionHistory(updatedUser.getPublicId().toString(),  ":Admin Updated User profile");
			return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(ApiResponse.error("Failed to update user: " + e.getMessage()));
		}
	}
	
	
    @PatchMapping("/admin/deactivateAccount")
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatusByPublicId(
            @RequestParam String publicId
    ) {
        try {
            User updated = userServices.updateAccountStatusByPublicId(UUID.fromString(publicId), 2);
            return ResponseEntity.ok("Account status updated for user with publicId: " + updated.getPublicId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PatchMapping("/admin/activateAccount")
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateAccount(
            @RequestParam String publicId
    ) {
        try {
            User updated = userServices.updateAccountStatusByPublicId(UUID.fromString(publicId), 1);
            return ResponseEntity.ok("Account status updated for user with publicId: " + updated.getPublicId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // For user preference setting
    
    @Operation(summary = "Get preferred job titles of logged-in user")
    @GetMapping("/preferences/job-titles")
    public ResponseEntity<ApiResponse<List<String>>> getPreferredJobTitles() {
        String publicId = jwtService.getPublicIdFromCurrentRequest();
        User user = userServices.getUserByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return ResponseEntity.ok(ApiResponse.success("Preferred job titles fetched", user.getPreferredJobTitles()));
    }

    @Operation(summary = "Add preferred job titles for logged-in user")
    @PatchMapping("/preferences/job-titles")
    public ResponseEntity<ApiResponse<List<String>>> addJobTitles(@RequestBody List<String> jobTitles) {
        String publicId = jwtService.getPublicIdFromCurrentRequest();
        List<String> result = userServices.addPreferredJobTitlesDynamically(UUID.fromString(publicId), jobTitles);
        return ResponseEntity.ok(ApiResponse.success("Preferred job titles updated", result));
    }

    @Operation(summary = "Delete one or all preferred job titles for logged-in user")
    @PatchMapping("/preferences/job-titles/delete")
    public ResponseEntity<ApiResponse<List<String>>> deleteJobTitles(@RequestParam(required = false) String jobTitle) {
        String publicId = jwtService.getPublicIdFromCurrentRequest();
        List<String> result = userServices.deletePreferredJobTitlesDynamically(UUID.fromString(publicId), jobTitle);
        return ResponseEntity.ok(ApiResponse.success("Preferred job titles updated", result));
    }
    
    // For gettting, saving Notification preference
    
    @Operation(summary = "Get notification preferences for logged-in user")
    @GetMapping("/preferences/notifications")
    public ResponseEntity<ApiResponse<Notification>> getNotificationPreferences() {
        String publicId = jwtService.getPublicIdFromCurrentRequest();
        User user = userServices.getUserByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Notification preferences fetched", user.getNotification()));
    }

    @Operation(summary = "Update notification preferences for logged-in user")
    @PatchMapping("/preferences/notifications")
    public ResponseEntity<ApiResponse<Notification>> updateNotificationPreferences(@RequestBody Notification newPrefs) {
        String publicId = jwtService.getPublicIdFromCurrentRequest();
        Notification updated = userServices.updateNotificationPreferences(UUID.fromString(publicId), newPrefs);
        return ResponseEntity.ok(ApiResponse.success("Notification preferences updated", updated));
    }

    @Operation(summary = "Delete notification preferences for logged-in user")
    @PatchMapping("/preferences/notifications/delete")
    public ResponseEntity<ApiResponse<String>> deleteNotificationPreferences() {
        String publicId = jwtService.getPublicIdFromCurrentRequest();
        boolean cleared = userServices.clearNotificationPreferences(UUID.fromString(publicId));
        if (cleared) {
            return ResponseEntity.ok(ApiResponse.success("Notification preferences deleted", null));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete notification preferences"));
        }
    }

    @Operation(summary = "Get notification preferences for any user by admin")
    @RolesAllowed("ROLE_ADMIN")
    @GetMapping("/admin/preferences/notifications")
    public ResponseEntity<ApiResponse<Notification>> getUserNotificationPreferenceByAdmin(@RequestParam String publicId) {
        User user = userServices.getUserByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Notification preferences fetched", user.getNotification()));
    }

    @Operation(summary = "Delete notification preferences for any user by admin")
    @RolesAllowed("ROLE_ADMIN")
    @PatchMapping("/admin/preferences/notifications/delete")
    public ResponseEntity<ApiResponse<String>> adminDeleteNotificationPreferences(@RequestParam String publicId) {
        boolean cleared = userServices.clearNotificationPreferences(UUID.fromString(publicId));
        if (cleared) {
            return ResponseEntity.ok(ApiResponse.success("Notification preferences deleted for user " + publicId, null));
        } else {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete notification preferences"));
        }
    }

}
