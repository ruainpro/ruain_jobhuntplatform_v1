package com.dao.rjobhunt.Controller.authentication;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.Service.PlatformService;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.Platform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Platform Management", description = "Manage scraping platforms: admin and user endpoints")
@RestController
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;
    
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private ActionHistoryServices actionHistoryServices;

    // ================================
    // ADMIN ROUTES
    // ================================

    @Operation(summary = "Admin: Get all platforms", description = "Admins see all platforms in the system.")
    @GetMapping("/api/admin/platforms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, java.util.List<Platform>>>> getAllPlatformsAdmin() {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = true;
        return platformService.getAllPlatforms(userId, isAdmin);
    }

    @Operation(summary = "Admin: Create a new platform", description = "Admins can create new platforms globally.")
    @PostMapping("/api/admin/platforms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Platform>> createPlatformAdmin(@RequestBody Platform platform) {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = true;
        actionHistoryServices.addActionHistory(userId,  ":Admin Created Platform");

        return platformService.createPlatform(platform, userId, isAdmin);
    }

    @Operation(summary = "Admin: Update a platform by publicId")
    @PutMapping("/api/admin/platforms/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Platform>> updatePlatformAdmin(@PathVariable String publicId,
                                                                     @RequestBody Platform updates) {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = true;
        actionHistoryServices.addActionHistory(userId,  publicId+" :Admin Updated Platform");
        return platformService.updatePlatform(publicId, updates, userId, isAdmin);
    }

    @Operation(summary = "Admin: Delete a platform by publicId")
    @DeleteMapping("/api/admin/platforms/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deletePlatformAdmin(@PathVariable String publicId) {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = true;
        actionHistoryServices.addActionHistory(userId,  publicId+" :Admin deleted Platform");
        return platformService.deletePlatform(publicId, userId, isAdmin);
    }

    // ================================
    // 👤 USER ROUTES
    // ================================

    @Operation(summary = "User: Get available platforms", description = "Users see standard platforms and their own custom platforms.")
    @GetMapping("/api/user/platforms")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, java.util.List<Platform>>>> getAllPlatformsUser() {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = false;
        return platformService.getAllPlatforms(userId, isAdmin);
    }

    @Operation(summary = "User: Create a custom platform", description = "Users can create their own custom platforms.")
    @PostMapping("/api/user/platforms")
    public ResponseEntity<ApiResponse<Platform>> createPlatformUser(@RequestBody Platform platform) {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = false;
        actionHistoryServices.addActionHistory(userId,   ":User deleted Platform");
        return platformService.createPlatform(platform, userId, isAdmin);
    }

    @Operation(summary = "User: Update own custom platform by publicId")
    @PutMapping("/api/user/platforms/{publicId}")
    public ResponseEntity<ApiResponse<Platform>> updatePlatformUser(@PathVariable String publicId,
                                                                    @RequestBody Platform updates) {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = false;
        actionHistoryServices.addActionHistory(userId,   publicId+ ":User Updated Platform");
        return platformService.updatePlatform(publicId, updates, userId, isAdmin);
    }

    @Operation(summary = "User: Delete own custom platform by publicId")
    @DeleteMapping("/api/user/platforms/{publicId}")
    public ResponseEntity<ApiResponse<String>> deletePlatformUser(@PathVariable String publicId) {
        String userId = jwtService.getPublicIdFromCurrentRequest();
        boolean isAdmin = false;
        actionHistoryServices.addActionHistory(userId,   publicId+ ":User Deleted Platform");
        return platformService.deletePlatform(publicId, userId, isAdmin);
    }
}