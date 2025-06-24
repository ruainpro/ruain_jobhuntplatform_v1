package com.dao.rjobhunt.Controller.authentication;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dao.rjobhunt.models.ActionHistory;
import com.dao.rjobhunt.repository.UserInfoRepository;
import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Action History", description = "APIs to fetch or search user and admin action history logs")
@RestController
@RequestMapping("/api/action_history")
public class ActionHistoryController {
	
	@Autowired
	private ActionHistoryServices actionHistoryServices;
	
	@Autowired
	private JwtService jwtService;

	@Operation(
		summary = "Get all action histories (Admin only)",
		description = "Requires ROLE_ADMIN to access full list of action histories"
	)
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<ActionHistory>>> getAllActions() {
		List<ActionHistory> actions = actionHistoryServices.getAllActions();
		return ResponseEntity.ok(ApiResponse.success("All actions retrieved", actions));
	}

	@Operation(
		summary = "Get user's own action history",
		description = "Returns actions related to the currently authenticated user"
	)
	@GetMapping("/user")
	public ResponseEntity<ApiResponse<List<ActionHistory>>> getUserActions() {
		String publicId = jwtService.getPublicIdFromCurrentRequest();
		List<ActionHistory> actions = actionHistoryServices.getActionsByUserId(publicId);
		return ResponseEntity.ok(ApiResponse.success("User actions retrieved", actions));
	}

	@Operation(
		summary = "Search user's action history by keyword",
		description = "Search action history using keywords like type, entity, description (USER only)"
	)
	@GetMapping("/search/by-user")
	public ResponseEntity<ApiResponse<List<ActionHistory>>> searchActionHistoryByUser(
			@RequestParam("q") String keyword) {
		try {
			String publicId = jwtService.getPublicIdFromCurrentRequest();
			List<ActionHistory> actions = actionHistoryServices.searchUserActionsFlexible(keyword, publicId);
			if (actions.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ApiResponse.error("No matching action history found for the user."));
			}
			return ResponseEntity.ok(ApiResponse.success("Actions fetched successfully", actions));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiResponse.error("Invalid publicId format or user not found."));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(ApiResponse.error("Internal error: " + e.getMessage()));
		}
	}
	
	
	@GetMapping("/search/projection")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
	    summary = "Paginated admin search with projection",
	    description = "Searches action history with joined user data (Mongo aggregation). Supports pagination. q= keyword to search, page  start from 1 and size= page size"
	)
	public ResponseEntity<ApiResponse<List<Document>>> searchActionsProjectionPaginated(
	        @RequestParam("q") String keyword,
	        @RequestParam(name = "page", defaultValue = "1") int page,
	        @RequestParam(name = "size", defaultValue = "10") int size) {
	    try {
	        List<Document> result = actionHistoryServices.searchActionsWithUserProjection(keyword, page, size);

	        if (result.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(ApiResponse.error("No matching action history found."));
	        }

	        return ResponseEntity.ok(ApiResponse.success("Action history retrieved with projection", result));
	    } catch (Exception e) {
	        return ResponseEntity.internalServerError()
	                .body(ApiResponse.error("Internal server error: " + e.getMessage()));
	    }
	}
}
